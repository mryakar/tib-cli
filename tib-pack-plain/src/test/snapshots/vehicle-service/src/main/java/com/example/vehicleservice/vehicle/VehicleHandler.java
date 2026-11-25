// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.Http;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;

/**
 * The API of vehicles, at the path of its context: /api/vehicles, and /api/vehicles/{id} for one of
 * them.
 */
public class VehicleHandler implements HttpHandler {

  private final VehicleService service;

  public VehicleHandler(VehicleService service) {
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
  private static VehicleDto dto(HttpExchange exchange) throws IOException {
    VehicleDto dto = Http.read(exchange, VehicleDto.class);
    Http.check(dto.problems());
    return dto;
  }
}
