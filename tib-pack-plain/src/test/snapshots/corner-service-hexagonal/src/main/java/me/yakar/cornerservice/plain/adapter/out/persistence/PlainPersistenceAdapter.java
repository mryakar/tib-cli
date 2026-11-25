// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.plain.application.port.out.PlainStore;
import me.yakar.cornerservice.plain.domain.Plain;

/**
 * The store of plains on their table, through the repository, whose SQL follows the spec. Every
 * call joins the transaction of the use case that makes it, and a failure of the database comes out
 * of it as a {@link Transactions.DatabaseException}: the store knows no JDBC.
 */
public class PlainPersistenceAdapter implements PlainStore {

  private final Transactions transactions;
  private final PlainRepository repository;

  public PlainPersistenceAdapter(Transactions transactions, PlainRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @Override
  public Page<Plain> findAll(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  @Override
  public Optional<Plain> findById(Long id) {
    return transactions.run(() -> repository.findById(id));
  }

  @Override
  public Plain insert(Plain plain) {
    return transactions.run(() -> repository.insert(plain));
  }

  @Override
  public void update(Plain plain) {
    transactions.run(() -> repository.update(plain));
  }

  @Override
  public void delete(Long id) {
    transactions.run(() -> repository.delete(id));
  }
}
