// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.yakar.cornerservice.branch.adapter.in.web.BranchHandler;
import me.yakar.cornerservice.branch.adapter.in.web.BranchWebMapper;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchPersistenceAdapter;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchRepository;
import me.yakar.cornerservice.branch.application.BranchService;
import me.yakar.cornerservice.item.adapter.in.web.ItemHandler;
import me.yakar.cornerservice.item.adapter.in.web.ItemWebMapper;
import me.yakar.cornerservice.item.adapter.out.persistence.ItemPersistenceAdapter;
import me.yakar.cornerservice.item.adapter.out.persistence.ItemRepository;
import me.yakar.cornerservice.item.application.ItemService;
import me.yakar.cornerservice.tag.adapter.in.web.TagHandler;
import me.yakar.cornerservice.tag.adapter.in.web.TagWebMapper;
import me.yakar.cornerservice.tag.adapter.out.persistence.TagPersistenceAdapter;
import me.yakar.cornerservice.tag.adapter.out.persistence.TagRepository;
import me.yakar.cornerservice.tag.application.TagService;

/**
 * The API of the entities of the spec, wired by hand: for every entity with a service, its
 * repository and the store on it, mapper, service and handler, the handler at the path of its
 * entities, behind {@link ErrorHandler}.
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    route(
        server,
        "/api/branches",
        new BranchHandler(
            new BranchService(
                transactions,
                new BranchPersistenceAdapter(transactions, new BranchRepository(transactions))),
            new BranchWebMapper()));
    route(
        server,
        "/api/tags",
        new TagHandler(
            new TagService(
                transactions,
                new TagPersistenceAdapter(transactions, new TagRepository(transactions))),
            new TagWebMapper()));
    route(
        server,
        "/api/items",
        new ItemHandler(
            new ItemService(
                transactions,
                new ItemPersistenceAdapter(transactions, new ItemRepository(transactions))),
            new ItemWebMapper()));
  }

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }
}
