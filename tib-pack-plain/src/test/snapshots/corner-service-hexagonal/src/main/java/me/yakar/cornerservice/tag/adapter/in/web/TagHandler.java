// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.adapter.in.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
import me.yakar.cornerservice.Http;
import me.yakar.cornerservice.tag.application.port.in.TagUseCase;
import me.yakar.cornerservice.tag.domain.Tag;

/**
 * The API of tags, at the path of its context: /api/tags, and /api/tags/{id} for one of them; their
 * DTOs, mapped to and from the domain.
 */
public class TagHandler implements HttpHandler {

  private final TagUseCase useCases;
  private final TagWebMapper mapper;

  public TagHandler(TagUseCase useCases, TagWebMapper mapper) {
    this.useCases = useCases;
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Optional<String> id = Http.id(exchange);
    if (id.isEmpty()) {
      switch (exchange.getRequestMethod()) {
        case "GET" ->
            Http.send(
                exchange,
                200,
                useCases
                    .list(Http.pageNumber(exchange), Http.pageSize(exchange))
                    .map(mapper::toDto));
        case "POST" -> Http.send(exchange, 201, create(dto(exchange)));
        default -> Http.methodNotAllowed(exchange, "GET, POST");
      }
    } else {
      Long key = Http.parse(id.get(), Long::valueOf);
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, mapper.toDto(useCases.get(key)));
        case "PUT" -> {
          TagDto dto = dto(exchange);
          Http.send(
              exchange, 200, mapper.toDto(useCases.update(key, tag -> mapper.update(dto, tag))));
        }
        case "DELETE" -> {
          useCases.delete(key);
          Http.noContent(exchange);
        }
        default -> Http.methodNotAllowed(exchange, "GET, PUT, DELETE");
      }
    }
  }

  /** A new tag, with the values of the DTO. */
  private TagDto create(TagDto dto) {
    Tag tag = new Tag();
    mapper.update(dto, tag);
    return mapper.toDto(useCases.create(tag));
  }

  /** The DTO in the body of the request, which has to keep the rules of the spec. */
  private static TagDto dto(HttpExchange exchange) throws IOException {
    TagDto dto = Http.read(exchange, TagDto.class);
    Http.check(dto.problems());
    return dto;
  }
}
