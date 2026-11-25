// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.item.domain.Item;

/** Where items are kept, as the application sees it: an outbound adapter implements it. */
public interface ItemStore {

  /** The page of items with the number, from 0, of the size. */
  Page<Item> findAll(int page, int size);

  Optional<Item> findById(Long id);

  /** Inserts the item, and gives it back as it was inserted, with its identifier. */
  Item insert(Item item);

  /** Updates the item, and says whether it is there. */
  boolean update(Item item);

  /** Deletes the item with the identifier, and says whether it was there. */
  boolean delete(Long id);
}
