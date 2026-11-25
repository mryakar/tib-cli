// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application.port.in;

import java.util.function.Consumer;
import me.yakar.cornerservice.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** The use cases of items, as the inbound adapters call them; the service implements them. */
public interface ItemUseCase {

  Page<Item> list(Pageable pageable);

  Item get(Long id);

  Item create(Item item);

  /** Changes the item as the change does, and saves it. */
  Item update(Long id, Consumer<Item> change);

  void delete(Long id);
}
