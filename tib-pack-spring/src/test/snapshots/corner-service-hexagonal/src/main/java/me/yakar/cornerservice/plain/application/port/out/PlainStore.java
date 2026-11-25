// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.plain.domain.Plain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Where plains are kept, as the application sees it: an outbound adapter implements it. */
public interface PlainStore {

  Page<Plain> findAll(Pageable pageable);

  Optional<Plain> findById(Long id);

  /** Saves the plain, and gives it back as it was saved, with its identifier. */
  Plain save(Plain plain);

  void delete(Plain plain);
}
