// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.yakar.cornerservice.branch.application.BranchService;
import me.yakar.cornerservice.branch.domain.BranchRepository;
import me.yakar.cornerservice.branch.web.BranchHandler;
import me.yakar.cornerservice.branch.web.BranchMapper;
import me.yakar.cornerservice.item.application.ItemService;
import me.yakar.cornerservice.item.domain.ItemRepository;
import me.yakar.cornerservice.item.web.ItemHandler;
import me.yakar.cornerservice.item.web.ItemMapper;
import me.yakar.cornerservice.tag.application.TagService;
import me.yakar.cornerservice.tag.domain.TagRepository;
import me.yakar.cornerservice.tag.web.TagHandler;
import me.yakar.cornerservice.tag.web.TagMapper;

/**
 * The API of the entities of the spec, wired by hand: for every entity with a service, its
 * repository, mapper, service and handler, the handler at the path of its entities, behind {@link
 * ErrorHandler}.
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    route(
        server,
        "/api/branches",
        new BranchHandler(
            new BranchService(transactions, new BranchRepository(transactions)),
            new BranchMapper()));
    route(
        server,
        "/api/tags",
        new TagHandler(
            new TagService(transactions, new TagRepository(transactions)), new TagMapper()));
    route(
        server,
        "/api/items",
        new ItemHandler(
            new ItemService(transactions, new ItemRepository(transactions)), new ItemMapper()));
  }

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }
}
