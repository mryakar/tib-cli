// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
import me.yakar.cornerservice.Http;
import me.yakar.cornerservice.branch.application.BranchService;
import me.yakar.cornerservice.branch.domain.Branch;

/**
 * The API of branches, at the path of its context: /api/branches, and /api/branches/{id} for one of
 * them; their DTOs, mapped to and from the aggregate.
 */
public class BranchHandler implements HttpHandler {

  private final BranchService service;
  private final BranchMapper mapper;

  public BranchHandler(BranchService service, BranchMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Optional<String> id = Http.id(exchange);
    if (id.isEmpty()) {
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, service.list().stream().map(mapper::toDto).toList());
        case "POST" -> Http.send(exchange, 201, create(dto(exchange)));
        default -> Http.methodNotAllowed(exchange, "GET, POST");
      }
    } else {
      Long key = Http.parse(id.get(), Long::valueOf);
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, mapper.toDto(service.get(key)));
        case "PUT" -> {
          BranchDto dto = dto(exchange);
          Http.send(
              exchange,
              200,
              mapper.toDto(service.update(key, branch -> mapper.update(dto, branch))));
        }
        case "DELETE" -> {
          service.delete(key);
          Http.noContent(exchange);
        }
        default -> Http.methodNotAllowed(exchange, "GET, PUT, DELETE");
      }
    }
  }

  /** A new branch, with the values of the DTO. */
  private BranchDto create(BranchDto dto) {
    Branch branch = new Branch();
    mapper.update(dto, branch);
    return mapper.toDto(service.create(branch));
  }

  /** The DTO in the body of the request, which has to keep the rules of the spec. */
  private static BranchDto dto(HttpExchange exchange) throws IOException {
    BranchDto dto = Http.read(exchange, BranchDto.class);
    Http.check(dto.problems());
    return dto;
  }
}
