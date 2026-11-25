// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.tag.domain.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Where tags are kept, as the application sees it: an outbound adapter implements it. */
public interface TagStore {

  Page<Tag> findAll(Pageable pageable);

  Optional<Tag> findById(Long id);

  /** Saves the tag, and gives it back as it was saved, with its identifier. */
  Tag save(Tag tag);

  void delete(Tag tag);
}
