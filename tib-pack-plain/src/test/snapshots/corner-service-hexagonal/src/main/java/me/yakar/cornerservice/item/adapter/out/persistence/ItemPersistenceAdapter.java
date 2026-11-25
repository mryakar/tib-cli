// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;

/**
 * The store of items on their table, through the repository, whose SQL follows the spec. Every call
 * joins the transaction of the use case that makes it, and a failure of the database comes out of
 * it as a {@link Transactions.DatabaseException}: the store knows no JDBC.
 */
public class ItemPersistenceAdapter implements ItemStore {

  private final Transactions transactions;
  private final ItemRepository repository;

  public ItemPersistenceAdapter(Transactions transactions, ItemRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @Override
  public Page<Item> findAll(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  @Override
  public Optional<Item> findById(Long id) {
    return transactions.run(() -> repository.findById(id));
  }

  @Override
  public Item insert(Item item) {
    return transactions.run(() -> repository.insert(item));
  }

  @Override
  public void update(Item item) {
    transactions.run(() -> repository.update(item));
  }

  @Override
  public void delete(Long id) {
    transactions.run(() -> repository.delete(id));
  }
}
