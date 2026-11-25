// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application;

import java.util.List;
import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.branch.domain.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The use cases of branches, on the aggregate; the web maps it to and from its DTO. */
@Service
@Transactional
public class BranchService {

  private final BranchRepository branches;

  public BranchService(BranchRepository branches) {
    this.branches = branches;
  }

  @Transactional(readOnly = true)
  public List<Branch> list() {
    return branches.findAll(org.springframework.data.domain.Sort.by("id"));
  }

  @Transactional(readOnly = true)
  public Branch get(Long id) {
    return find(id);
  }

  public Branch create(Branch branch) {
    return branches.saveAndFlush(branch);
  }

  /** Changes the branch as the change does, and saves it. */
  public Branch update(Long id, Consumer<Branch> change) {
    Branch branch = find(id);
    change.accept(branch);
    return branches.saveAndFlush(branch);
  }

  public void delete(Long id) {
    branches.delete(find(id));
  }

  private Branch find(Long id) {
    return branches
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no branch " + id));
  }
}
