// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application.port.in;

import java.util.List;
import java.util.function.Consumer;
import me.yakar.cornerservice.branch.domain.Branch;

/** The use cases of branches, as the inbound adapters call them; the service implements them. */
public interface BranchUseCase {

  List<Branch> list();

  Branch get(Long id);

  Branch create(Branch branch);

  /** Changes the branch as the change does, and saves it. */
  Branch update(Long id, Consumer<Branch> change);

  void delete(Long id);
}
