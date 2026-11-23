// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** The store of items on their table, through JPA; the mapper maps them to and from the domain. */
@Component
public class ItemPersistenceAdapter implements ItemStore {

  private final ItemJpaRepository items;
  private final ItemPersistenceMapper mapper;

  public ItemPersistenceAdapter(ItemJpaRepository items, ItemPersistenceMapper mapper) {
    this.items = items;
    this.mapper = mapper;
  }

  @Override
  public Page<Item> findAll(Pageable pageable) {
    return items.findAll(pageable).map(mapper::toDomain);
  }

  @Override
  public Optional<Item> findById(Long id) {
    return items.findById(id).map(mapper::toDomain);
  }

  /** Saves the item and flushes it, so that a broken constraint shows here. */
  @Override
  public Item save(Item item) {
    return mapper.toDomain(items.saveAndFlush(mapper.toEntity(item)));
  }

  @Override
  public void delete(Item item) {
    items.deleteById(item.getId());
  }
}
