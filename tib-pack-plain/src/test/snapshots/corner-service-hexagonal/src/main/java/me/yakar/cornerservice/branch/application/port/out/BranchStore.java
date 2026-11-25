// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application.port.out;

import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.branch.domain.Branch;

/** Where branches are kept, as the application sees it: an outbound adapter implements it. */
public interface BranchStore {

  List<Branch> findAll();

  Optional<Branch> findById(Long id);

  /**
   * Inserts the branch, and gives it back as it was inserted, with its identifier and its audit
   * times.
   */
  Branch insert(Branch branch);

  /** Updates the branch, and says whether it is there. */
  boolean update(Branch branch);

  /** Deletes the branch with the identifier, and says whether it was there. */
  boolean delete(Long id);
}
