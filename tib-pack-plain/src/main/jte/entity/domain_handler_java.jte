@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
!{boolean hexagonal = entity.hexagonal();}
!{String useCase = name + (hexagonal ? "UseCase" : "Service");}
!{String useCasePackage = hexagonal ? entity.useCasePackage() : entity.applicationPackage();}
!{String useCases = hexagonal ? "useCases" : "service";}
package ${entity.webPackage()};

import ${useCasePackage}.${useCase};
import ${entity.basePackage()}.Http;
import ${entity.domainPackage()}.${name};
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif

/**
 * The API of ${entity.plural()}, at the path of its context: ${entity.path()}, and ${entity.path()}/{id} for one of
 * them; their DTOs, mapped to and from the ${hexagonal ? "domain" : "aggregate"}.
 */
public class ${name}Handler implements HttpHandler {

  private final ${useCase} ${useCases};
  private final ${entity.webMapper()} mapper;

  public ${name}Handler(${useCase} ${useCases}, ${entity.webMapper()} mapper) {
    this.${useCases} = ${useCases};
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Optional<String> id = Http.id(exchange);
    if (id.isEmpty()) {
      switch (exchange.getRequestMethod()) {
        @if(entity.paginated())
        case "GET" ->
            Http.send(exchange, 200, ${useCases}.list(Http.pageNumber(exchange), Http.pageSize(exchange)).map(mapper::toDto));
        @else
        case "GET" -> Http.send(exchange, 200, ${useCases}.list().stream().map(mapper::toDto).toList());
        @endif
        case "POST" -> Http.send(exchange, 201, create(dto(exchange)));
        default -> Http.methodNotAllowed(exchange, "GET, POST");
      }
    } else {
      ${entity.idType()} key = Http.parse(id.get(), ${entity.uuidId() ? "UUID::fromString" : "Long::valueOf"});
      switch (exchange.getRequestMethod()) {
        case "GET" -> Http.send(exchange, 200, mapper.toDto(${useCases}.get(key)));
        case "PUT" -> {
          ${name}Dto dto = dto(exchange);
          Http.send(exchange, 200, mapper.toDto(${useCases}.update(key, ${entity.variable()} -> mapper.update(dto, ${entity.variable()}))));
        }
        case "DELETE" -> {
          ${useCases}.delete(key);
          Http.noContent(exchange);
        }
        default -> Http.methodNotAllowed(exchange, "GET, PUT, DELETE");
      }
    }
  }

  /** A new ${entity.variable()}, with the values of the DTO. */
  private ${name}Dto create(${name}Dto dto) {
    ${name} ${entity.variable()} = new ${name}();
    mapper.update(dto, ${entity.variable()});
    return mapper.toDto(${useCases}.create(${entity.variable()}));
  }

  /** The DTO in the body of the request, which has to keep the rules of the spec. */
  private static ${name}Dto dto(HttpExchange exchange) throws IOException {
    ${name}Dto dto = Http.read(exchange, ${name}Dto.class);
    Http.check(dto.problems());
    return dto;
  }
}
