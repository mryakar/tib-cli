// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.application.ItemService;
import me.yakar.cornerservice.item.domain.ItemRepository;
import me.yakar.cornerservice.item.web.ItemHandler;
import me.yakar.cornerservice.item.web.ItemMapper;

/**
 * The classes of items, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class ItemWiring {

  private ItemWiring() {}

  /** The handler of the API of items, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    ItemRepository repository = new ItemRepository(transactions);
    ItemService service = new ItemService(transactions, repository);
    return new ItemHandler(service, new ItemMapper());
  }
}
