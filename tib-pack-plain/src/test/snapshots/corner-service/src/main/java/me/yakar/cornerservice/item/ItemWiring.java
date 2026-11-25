// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;

/**
 * The classes of items, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class ItemWiring {

  private ItemWiring() {}

  /** The handler of the API of items, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    ItemRepository repository = new ItemRepository(transactions);
    ItemService service = new ItemService(transactions, repository, new ItemMapper());
    return new ItemHandler(service);
  }
}
