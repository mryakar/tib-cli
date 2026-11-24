// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import java.sql.SQLException;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;

/** The use cases of items, each in a transaction, on their DTOs. */
public class ItemService {

  private final Transactions transactions;
  private final ItemRepository repository;
  private final ItemMapper mapper;

  public ItemService(Transactions transactions, ItemRepository repository, ItemMapper mapper) {
    this.transactions = transactions;
    this.repository = repository;
    this.mapper = mapper;
  }

  /** The page of items with the number, from 0, of the size. */
  public Page<ItemDto> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size).map(mapper::toDto));
  }

  public ItemDto get(Long id) {
    return transactions.run(() -> mapper.toDto(find(id)));
  }

  public ItemDto create(ItemDto dto) {
    return transactions.run(
        () -> {
          Item item = new Item();
          mapper.update(dto, item);
          return mapper.toDto(repository.insert(item));
        });
  }

  public ItemDto update(Long id, ItemDto dto) {
    return transactions.run(
        () -> {
          Item item = find(id);
          mapper.update(dto, item);
          repository.update(item);
          return mapper.toDto(item);
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
