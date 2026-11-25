// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import java.sql.SQLException;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;

/** The use cases of tags, each in a transaction, on their DTOs. */
public class TagService {

  private final Transactions transactions;
  private final TagRepository repository;
  private final TagMapper mapper;

  public TagService(Transactions transactions, TagRepository repository, TagMapper mapper) {
    this.transactions = transactions;
    this.repository = repository;
    this.mapper = mapper;
  }

  /** The page of tags with the number, from 0, of the size. */
  public Page<TagDto> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size).map(mapper::toDto));
  }

  public TagDto get(Long id) {
    return transactions.run(() -> mapper.toDto(find(id)));
  }

  public TagDto create(TagDto dto) {
    return transactions.run(() -> {
      Tag tag = new Tag();
      mapper.update(dto, tag);
      return mapper.toDto(repository.insert(tag));
    });
  }

  public TagDto update(Long id, TagDto dto) {
    return transactions.run(() -> {
      Tag tag = find(id);
      mapper.update(dto, tag);
      if (!repository.update(tag)) {
        throw notFound(id);
      }
      return mapper.toDto(tag);
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
