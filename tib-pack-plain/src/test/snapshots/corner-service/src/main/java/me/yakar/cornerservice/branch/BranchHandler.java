// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
import me.yakar.cornerservice.Http;

/**
 * The API of branches, at the path of its context: /api/branches, and /api/branches/{id} for one of
 * them.
 */
public class BranchHandler implements HttpHandler {

  private final BranchService service;

  public BranchHandler(BranchService service) {
    this.service = service;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Optional<String> id = Http.id(exchange);
    if (id.isEmpty()) {
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, service.list());
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
  private static BranchDto dto(HttpExchange exchange) throws IOException {
    BranchDto dto = Http.read(exchange, BranchDto.class);
    Http.check(dto.problems());
    return dto;
  }
}
