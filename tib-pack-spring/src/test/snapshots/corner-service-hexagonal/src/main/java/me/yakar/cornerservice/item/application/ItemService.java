// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application;

import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.item.application.port.in.ItemUseCase;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of items, on the domain, which the store keeps; the adapters know the rest. */
@Service
@Transactional
public class ItemService implements ItemUseCase {

  private final ItemStore items;

  public ItemService(ItemStore items) {
    this.items = items;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Item> list(Pageable pageable) {
    return items.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Item get(Long id) {
    return find(id);
  }

  @Override
  public Item create(Item item) {
    return items.save(item);
  }

  @Override
  public Item update(Long id, Consumer<Item> change) {
    Item item = find(id);
    change.accept(item);
    return items.save(item);
  }

  @Override
  public void delete(Long id) {
    items.delete(find(id));
  }

  private Item find(Long id) {
    return items.findById(id).orElseThrow(() -> new NotFoundException("There is no item " + id));
  }
}
