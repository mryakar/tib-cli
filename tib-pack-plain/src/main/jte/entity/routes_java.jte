@import java.util.List
@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.spec.Architecture
@param Project project
@param List<EntityModel> entities
package ${project.basePackage()};

@for(var entity : entities)
import ${entity.webPackage()}.${entity.name()}Handler;
import ${entity.webPackage()}.${entity.name()}Mapper;
import ${entity.persistencePackage()}.${entity.name()}Repository;
import ${entity.applicationPackage()}.${entity.name()}Service;
@endfor
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * The API of the entities of the spec, wired by hand: for every entity with a service, its
 * repository, mapper, service and handler, the handler at the path of its entities@if(!entities.isEmpty()), behind {@link ErrorHandler}@endif.
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    @for(var entity : entities)
    @if(project.architecture() == Architecture.ANEMIC)
    route(
        server,
        "${entity.path()}",
        new ${entity.name()}Handler(
            new ${entity.name()}Service(transactions, new ${entity.name()}Repository(transactions), new ${entity.name()}Mapper())));
    @else
    route(
        server,
        "${entity.path()}",
        new ${entity.name()}Handler(
            new ${entity.name()}Service(transactions, new ${entity.name()}Repository(transactions)), new ${entity.name()}Mapper()));
    @endif
    @endfor
  }
  @if(!entities.isEmpty())<%-- keeps the blank line below --%>

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }
  @endif
}
