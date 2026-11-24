@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.packageName()};

import ${entity.basePackage()}.Http;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif

/**
 * The API of ${entity.plural()}, at the path of its context: ${entity.path()}, and ${entity.path()}/{id} for one of
 * them. A request whose DTO does not read or breaks a rule is a bad request, and an identifier that
 * no ${entity.variable()} has is not found: {@code ErrorHandler} answers them.
 */
public class ${name}Handler implements HttpHandler {

  private final ${name}Service service;

  public ${name}Handler(${name}Service service) {
    this.service = service;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Optional<String> id = Http.id(exchange);
    if (id.isEmpty()) {
      switch (exchange.getRequestMethod()) {
        @if(entity.paginated())
        case "GET" -> Http.send(exchange, 200, service.list(Http.pageNumber(exchange), Http.pageSize(exchange)));
        @else
        case "GET" -> Http.send(exchange, 200, service.list());
        @endif
        case "POST" -> Http.send(exchange, 201, service.create(dto(exchange)));
        default -> Http.methodNotAllowed(exchange, "GET, POST");
      }
    } else {
      ${entity.idType()} key = Http.parse(id.get(), ${entity.uuidId() ? "UUID::fromString" : "Long::valueOf"});
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
  private static ${name}Dto dto(HttpExchange exchange) throws IOException {
    ${name}Dto dto = Http.read(exchange, ${name}Dto.class);
    Http.check(dto.problems());
    return dto;
  }
}
