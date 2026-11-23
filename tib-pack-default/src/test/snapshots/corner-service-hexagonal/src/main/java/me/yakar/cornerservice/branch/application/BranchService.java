// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application;

import java.util.List;
import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.branch.application.port.in.BranchUseCase;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of branches, on the domain, which the store keeps; the adapters know the rest. */
@Service
@Transactional
public class BranchService implements BranchUseCase {

  private final BranchStore branches;

  public BranchService(BranchStore branches) {
    this.branches = branches;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Branch> list() {
    return branches.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Branch get(Long id) {
    return find(id);
  }

  @Override
  public Branch create(Branch branch) {
    return branches.save(branch);
  }

  @Override
  public Branch update(Long id, Consumer<Branch> change) {
    Branch branch = find(id);
    change.accept(branch);
    return branches.save(branch);
  }

  @Override
  public void delete(Long id) {
    branches.delete(find(id));
  }

  private Branch find(Long id) {
    return branches
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no branch " + id));
  }
}
