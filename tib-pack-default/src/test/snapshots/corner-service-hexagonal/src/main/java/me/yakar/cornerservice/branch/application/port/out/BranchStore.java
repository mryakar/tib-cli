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
   * Saves the branch, and gives it back as it was saved, with its identifier and its audit times.
   */
  Branch save(Branch branch);

  void delete(Branch branch);
}
