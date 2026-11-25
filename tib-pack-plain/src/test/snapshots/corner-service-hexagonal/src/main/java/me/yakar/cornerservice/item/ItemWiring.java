// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.adapter.in.web.ItemHandler;
import me.yakar.cornerservice.item.adapter.in.web.ItemWebMapper;
import me.yakar.cornerservice.item.adapter.out.persistence.ItemPersistenceAdapter;
import me.yakar.cornerservice.item.adapter.out.persistence.ItemRepository;
import me.yakar.cornerservice.item.application.ItemService;
import me.yakar.cornerservice.item.application.port.out.ItemStore;

/**
 * The classes of items, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class ItemWiring {

  private ItemWiring() {}

  /** The handler of the API of items, on their use cases, which keep them in the store. */
  public static HttpHandler handler(Transactions transactions) {
    ItemRepository repository = new ItemRepository(transactions);
    ItemStore store = new ItemPersistenceAdapter(transactions, repository);
    ItemService service = new ItemService(transactions, store);
    return new ItemHandler(service, new ItemWebMapper());
  }
}
