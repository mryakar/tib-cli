// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;

/**
 * The store of tags on their table, through the repository, whose SQL follows the spec. Every call
 * joins the transaction of the use case that makes it, and a failure of the database comes out of
 * it as a {@link Transactions.DatabaseException}: the store knows no JDBC.
 */
public class TagPersistenceAdapter implements TagStore {

  private final Transactions transactions;
  private final TagRepository repository;

  public TagPersistenceAdapter(Transactions transactions, TagRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @Override
  public Page<Tag> findAll(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  @Override
  public Optional<Tag> findById(Long id) {
    return transactions.run(() -> repository.findById(id));
  }

  @Override
  public Tag insert(Tag tag) {
    return transactions.run(() -> repository.insert(tag));
  }

  @Override
  public void update(Tag tag) {
    transactions.run(() -> repository.update(tag));
  }

  @Override
  public void delete(Long id) {
    transactions.run(() -> repository.delete(id));
  }
}
