@import java.util.List
@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.EntityModel
@param Project project
@param List<EntityModel> entities
package ${project.basePackage()};

@for(var entity : entities)
import ${entity.packageName()}.${entity.name()}Wiring;
@endfor
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * The API of the entities of the spec: for every entity with a service, the handler that its
 * wiring makes, at the path of its entities.
 @if(!entities.isEmpty())
 * A path that none of them has is not found. {@link ErrorHandler} answers what a handler throws.
 @endif
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    @for(var entity : entities)
    route(server, "${entity.path()}", ${entity.name()}Wiring.handler(transactions));
    @endfor
    @if(!entities.isEmpty())
    route(server, "/", Routes::nothing);
    @endif
  }
  @if(!entities.isEmpty())

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }

  /** Every path that no entity has. */
  private static void nothing(HttpExchange exchange) {
    throw new NotFoundException("There is nothing at " + exchange.getRequestURI().getPath());
  }
  @endif
}
