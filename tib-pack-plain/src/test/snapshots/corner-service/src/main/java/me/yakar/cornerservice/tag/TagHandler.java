// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
import me.yakar.cornerservice.Http;

/**
 * The API of tags, at the path of its context: /api/tags, and /api/tags/{id} for one of them. A
 * request whose DTO does not read or breaks a rule is a bad request, and an identifier that no tag
 * has is not found: {@code ErrorHandler} answers them.
 */
public class TagHandler implements HttpHandler {

  private final TagService service;

  public TagHandler(TagService service) {
    this.service = service;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Optional<String> id = Http.id(exchange);
    if (id.isEmpty()) {
      switch (exchange.getRequestMethod()) {
        case "GET" ->
            Http.send(
                exchange, 200, service.list(Http.pageNumber(exchange), Http.pageSize(exchange)));
        case "POST" -> Http.send(exchange, 201, service.create(dto(exchange)));
        default -> Http.methodNotAllowed(exchange, "GET, POST");
      }
    } else {
      Long key = Http.parse(id.get(), Long::valueOf);
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, service.get(key));
        case "PUT" -> Http.send(exchange, 200, service.update(key, dto(exchange)));
        case "DELETE" -> {
          service.delete(key);
          Http.noContent(exchange);
        }
        default -> Http.methodNotAllowed(exchange, "GET, PUT, DELETE");
      }
    }
  }

  /** The DTO in the body of the request, which has to keep the rules of the spec. */
  private static TagDto dto(HttpExchange exchange) throws IOException {
    TagDto dto = Http.read(exchange, TagDto.class);
    Http.check(dto.problems());
    return dto;
  }
}
