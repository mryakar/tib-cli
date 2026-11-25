// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application;

import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.application.port.in.ItemUseCase;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;

/**
 * The use cases of items, on the domain, which the store keeps, each in a transaction; the adapters
 * know the rest.
 */
public class ItemService implements ItemUseCase {

  private final Transactions transactions;
  private final ItemStore store;

  public ItemService(Transactions transactions, ItemStore store) {
    this.transactions = transactions;
    this.store = store;
  }

  @Override
  public Page<Item> list(int page, int size) {
    return transactions.run(() -> store.findAll(page, size));
  }

  @Override
  public Item get(Long id) {
    return transactions.run(() -> find(id));
  }

  @Override
  public Item create(Item item) {
    return transactions.run(() -> store.insert(item));
  }

  @Override
  public Item update(Long id, Consumer<Item> change) {
    return transactions.run(
        () -> {
          Item item = find(id);
          change.accept(item);
          if (!store.update(item)) {
            throw notFound(id);
          }
          return item;
        });
  }

  @Override
  public void delete(Long id) {
    transactions.run(
        () -> {
          if (!store.delete(id)) {
            throw notFound(id);
          }
          return null;
        });
  }

  private Item find(Long id) {
    return store.findById(id).orElseThrow(() -> notFound(id));
  }

  private static NotFoundException notFound(Long id) {
    return new NotFoundException("There is no item " + id);
  }
}
