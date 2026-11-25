// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.tag.domain.Tag;

/** Where tags are kept, as the application sees it: an outbound adapter implements it. */
public interface TagStore {

  /** The page of tags with the number, from 0, of the size. */
  Page<Tag> findAll(int page, int size);

  Optional<Tag> findById(Long id);

  /** Inserts the tag, and gives it back as it was inserted, with its identifier. */
  Tag insert(Tag tag);

  /** Updates the tag, and says whether it is there. */
  boolean update(Tag tag);

  /** Deletes the tag with the identifier, and says whether it was there. */
  boolean delete(Long id);
}
