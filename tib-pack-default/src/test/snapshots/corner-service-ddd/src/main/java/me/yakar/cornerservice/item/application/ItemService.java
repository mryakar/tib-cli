// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application;

import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.item.domain.Item;
import me.yakar.cornerservice.item.domain.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of items, on the aggregate; the web maps it to and from its DTO. */
@Service
@Transactional
public class ItemService {

  private final ItemRepository items;

  public ItemService(ItemRepository items) {
    this.items = items;
  }

  @Transactional(readOnly = true)
  public Page<Item> list(Pageable pageable) {
    return items.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Item get(Long id) {
    return find(id);
  }

  public Item create(Item item) {
    return items.saveAndFlush(item);
  }

  /** Changes the item as the change does, and saves it. */
  public Item update(Long id, Consumer<Item> change) {
    Item item = find(id);
    change.accept(item);
    return items.saveAndFlush(item);
  }

  public void delete(Long id) {
    items.delete(find(id));
  }

  private Item find(Long id) {
    return items.findById(id).orElseThrow(() -> new NotFoundException("There is no item " + id));
  }
}
