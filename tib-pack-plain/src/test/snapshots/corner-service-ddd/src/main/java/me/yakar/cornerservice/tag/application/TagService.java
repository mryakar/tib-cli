// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application;

import java.sql.SQLException;
import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.tag.domain.Tag;
import me.yakar.cornerservice.tag.domain.TagRepository;

/**
 * The use cases of tags, on the aggregate, each in a transaction; the web maps it to and from its
 * DTO.
 */
public class TagService {

  private final Transactions transactions;
  private final TagRepository repository;

  public TagService(Transactions transactions, TagRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  /** The page of tags with the number, from 0, of the size. */
  public Page<Tag> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  public Tag get(Long id) {
    return transactions.run(() -> find(id));
  }

  public Tag create(Tag tag) {
    return transactions.run(() -> repository.insert(tag));
  }

  /** Changes the tag as the change does, and saves it. */
  public Tag update(Long id, Consumer<Tag> change) {
    return transactions.run(() -> {
      Tag tag = find(id);
      change.accept(tag);
      if (!repository.update(tag)) {
        throw notFound(id);
      }
      return tag;
    });
  }

  public void delete(Long id) {
    transactions.run(() -> {
      if (!repository.delete(id)) {
        throw notFound(id);
      }
      return null;
    });
  }

  private Tag find(Long id) throws SQLException {
    return repository.findById(id).orElseThrow(() -> notFound(id));
  }

  private static NotFoundException notFound(Long id) {
    return new NotFoundException("There is no tag " + id);
  }
}
