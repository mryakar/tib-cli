// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
import me.yakar.cornerservice.Http;
import me.yakar.cornerservice.item.application.ItemService;
import me.yakar.cornerservice.item.domain.Item;

/**
 * The API of items, at the path of its context: /api/items, and /api/items/{id} for one of them;
 * their DTOs, mapped to and from the aggregate. A request whose DTO does not read or breaks a rule
 * is a bad request, and an identifier that no item has is not found: {@code ErrorHandler} answers
 * them.
 */
public class ItemHandler implements HttpHandler {

  private final ItemService service;
  private final ItemMapper mapper;

  public ItemHandler(ItemService service, ItemMapper mapper) {
    this.service = service;
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
                service
                    .list(Http.pageNumber(exchange), Http.pageSize(exchange))
                    .map(mapper::toDto));
        case "POST" -> Http.send(exchange, 201, create(dto(exchange)));
        default -> Http.methodNotAllowed(exchange, "GET, POST");
      }
    } else {
      Long key = Http.parse(id.get(), Long::valueOf);
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, mapper.toDto(service.get(key)));
        case "PUT" -> {
          ItemDto dto = dto(exchange);
          Http.send(
              exchange, 200, mapper.toDto(service.update(key, item -> mapper.update(dto, item))));
        }
        case "DELETE" -> {
          service.delete(key);
          Http.noContent(exchange);
        }
        default -> Http.methodNotAllowed(exchange, "GET, PUT, DELETE");
      }
    }
  }

  /** A new item, with the values of the DTO. */
  private ItemDto create(ItemDto dto) {
    Item item = new Item();
    mapper.update(dto, item);
    return mapper.toDto(service.create(item));
  }

  /** The DTO in the body of the request, which has to keep the rules of the spec. */
  private static ItemDto dto(HttpExchange exchange) throws IOException {
    ItemDto dto = Http.read(exchange, ItemDto.class);
    Http.check(dto.problems());
    return dto;
  }
}
