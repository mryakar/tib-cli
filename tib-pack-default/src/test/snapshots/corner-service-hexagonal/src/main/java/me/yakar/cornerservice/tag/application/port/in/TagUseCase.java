// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.application.port.in;

import java.util.function.Consumer;
import me.yakar.cornerservice.tag.domain.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** The use cases of tags, as the inbound adapters call them; the service implements them. */
public interface TagUseCase {

  Page<Tag> list(Pageable pageable);

  Tag get(Long id);

  Tag create(Tag tag);

  /** Changes the tag as the change does, and saves it. */
  Tag update(Long id, Consumer<Tag> change);

  void delete(Long id);
}
