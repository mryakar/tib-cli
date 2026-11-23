// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Where items are kept, as the application sees it: an outbound adapter implements it. */
public interface ItemStore {

  Page<Item> findAll(Pageable pageable);

  Optional<Item> findById(Long id);

  /** Saves the item, and gives it back as it was saved, with its identifier. */
  Item save(Item item);

  void delete(Item item);
}
