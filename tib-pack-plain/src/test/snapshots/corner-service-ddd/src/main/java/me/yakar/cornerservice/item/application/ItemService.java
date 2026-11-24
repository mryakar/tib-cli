// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application;

import java.sql.SQLException;
import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.domain.Item;
import me.yakar.cornerservice.item.domain.ItemRepository;

/**
 * The use cases of items, on the aggregate, each in a transaction; the web maps it to and from its
 * DTO.
 */
public class ItemService {

  private final Transactions transactions;
  private final ItemRepository repository;

  public ItemService(Transactions transactions, ItemRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  /** The page of items with the number, from 0, of the size. */
  public Page<Item> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  public Item get(Long id) {
    return transactions.run(() -> find(id));
  }

  public Item create(Item item) {
    return transactions.run(() -> repository.insert(item));
  }

  /** Changes the item as the change does, and saves it. */
  public Item update(Long id, Consumer<Item> change) {
    return transactions.run(
        () -> {
          Item item = find(id);
          change.accept(item);
          repository.update(item);
          return item;
        });
  }

  public void delete(Long id) {
    transactions.run(
        () -> {
          repository.delete(find(id).getId());
          return null;
        });
  }

  private Item find(Long id) throws SQLException {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no item " + id));
  }
}
