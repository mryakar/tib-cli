// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.branch.domain.BranchRepository;

/**
 * The use cases of branches, on the aggregate, each in a transaction; the web maps it to and from
 * its DTO.
 */
public class BranchService {

  private final Transactions transactions;
  private final BranchRepository repository;

  public BranchService(Transactions transactions, BranchRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  public List<Branch> list() {
    return transactions.run(repository::findAll);
  }

  public Branch get(Long id) {
    return transactions.run(() -> find(id));
  }

  public Branch create(Branch branch) {
    return transactions.run(() -> repository.insert(branch));
  }

  /** Changes the branch as the change does, and saves it. */
  public Branch update(Long id, Consumer<Branch> change) {
    return transactions.run(() -> {
      Branch branch = find(id);
      change.accept(branch);
      if (!repository.update(branch)) {
        throw notFound(id);
      }
      return branch;
    });
  }

  public void delete(Long id) {
    transactions.run(() -> {
      if (!repository.delete(id)) {
        throw notFound(id);
      }
      return null;
    });
  }

  private Branch find(Long id) throws SQLException {
    return repository.findById(id).orElseThrow(() -> notFound(id));
  }

  private static NotFoundException notFound(Long id) {
    return new NotFoundException("There is no branch " + id);
  }
}
