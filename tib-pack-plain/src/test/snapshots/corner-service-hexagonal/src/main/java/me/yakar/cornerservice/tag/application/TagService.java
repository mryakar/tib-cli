// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.tag.application.port.in.TagUseCase;
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;

/**
 * The use cases of tags, on the domain, which the store keeps, each in a transaction; the adapters
 * know the rest.
 */
public class TagService implements TagUseCase {

  private final Transactions transactions;
  private final TagStore store;

  public TagService(Transactions transactions, TagStore store) {
    this.transactions = transactions;
    this.store = store;
  }

  @Override
  public Page<Tag> list(int page, int size) {
    return transactions.run(() -> store.findAll(page, size));
  }

  @Override
  public Tag get(Long id) {
    return transactions.run(() -> find(id));
  }

  @Override
  public Tag create(Tag tag) {
    return transactions.run(() -> store.insert(tag));
  }

  @Override
  public Tag update(Long id, Consumer<Tag> change) {
    return transactions.run(() -> {
      Tag tag = find(id);
      change.accept(tag);
      if (!store.update(tag)) {
        throw notFound(id);
      }
      return tag;
    });
  }

  @Override
  public void delete(Long id) {
    transactions.run(() -> {
      if (!store.delete(id)) {
        throw notFound(id);
      }
      return null;
    });
  }

  private Tag find(Long id) {
    return store.findById(id).orElseThrow(() -> notFound(id));
  }

  private static NotFoundException notFound(Long id) {
    return new NotFoundException("There is no tag " + id);
  }
}
