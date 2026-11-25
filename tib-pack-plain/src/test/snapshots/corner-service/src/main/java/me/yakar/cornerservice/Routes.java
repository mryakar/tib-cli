// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.yakar.cornerservice.branch.BranchWiring;
import me.yakar.cornerservice.item.ItemWiring;
import me.yakar.cornerservice.tag.TagWiring;

/**
 * The API of the entities of the spec: for every entity with a service, the handler that its wiring
 * makes, at the path of its entities. A path that none of them has is not found. {@link
 * ErrorHandler} answers what a handler throws.
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    route(server, "/api/branches", BranchWiring.handler(transactions));
    route(server, "/api/tags", TagWiring.handler(transactions));
    route(server, "/api/items", ItemWiring.handler(transactions));
    route(server, "/", Routes::nothing);
  }

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }

  /** Every path that no entity has. */
  private static void nothing(HttpExchange exchange) {
    throw new NotFoundException("There is nothing at " + exchange.getRequestURI().getPath());
  }
}
