// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application;

import java.util.List;
import java.util.function.Consumer;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.application.port.in.BranchUseCase;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;

/**
 * The use cases of branches, on the domain, which the store keeps, each in a transaction; the
 * adapters know the rest.
 */
public class BranchService implements BranchUseCase {

  private final Transactions transactions;
  private final BranchStore store;

  public BranchService(Transactions transactions, BranchStore store) {
    this.transactions = transactions;
    this.store = store;
  }

  @Override
  public List<Branch> list() {
    return transactions.run(store::findAll);
  }

  @Override
  public Branch get(Long id) {
    return transactions.run(() -> find(id));
  }

  @Override
  public Branch create(Branch branch) {
    return transactions.run(() -> store.insert(branch));
  }

  @Override
  public Branch update(Long id, Consumer<Branch> change) {
    return transactions.run(
        () -> {
          Branch branch = find(id);
          change.accept(branch);
          store.update(branch);
          return branch;
        });
  }

  @Override
  public void delete(Long id) {
    transactions.run(
        () -> {
          store.delete(find(id).getId());
          return null;
        });
  }

  private Branch find(Long id) {
    return store.findById(id).orElseThrow(() -> new NotFoundException("There is no branch " + id));
  }
}
