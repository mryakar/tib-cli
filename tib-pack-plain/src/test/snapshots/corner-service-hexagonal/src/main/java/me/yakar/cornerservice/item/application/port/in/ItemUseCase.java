// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application.port.in;

import java.util.function.Consumer;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.item.domain.Item;

/** The use cases of items, as the inbound adapters call them; the service implements them. */
public interface ItemUseCase {

  /** The page of items with the number, from 0, of the size. */
  Page<Item> list(int page, int size);

  Item get(Long id);

  Item create(Item item);

  /** Changes the item as the change does, and saves it. */
  Item update(Long id, Consumer<Item> change);

  void delete(Long id);
}
