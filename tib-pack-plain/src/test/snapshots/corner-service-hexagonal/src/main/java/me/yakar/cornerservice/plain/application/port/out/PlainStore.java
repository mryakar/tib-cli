// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.plain.domain.Plain;

/** Where plains are kept, as the application sees it: an outbound adapter implements it. */
public interface PlainStore {

  /** The page of plains with the number, from 0, of the size. */
  Page<Plain> findAll(int page, int size);

  Optional<Plain> findById(Long id);

  /** Inserts the plain, and gives it back as it was inserted, with its identifier. */
  Plain insert(Plain plain);

  void update(Plain plain);

  void delete(Long id);
}
