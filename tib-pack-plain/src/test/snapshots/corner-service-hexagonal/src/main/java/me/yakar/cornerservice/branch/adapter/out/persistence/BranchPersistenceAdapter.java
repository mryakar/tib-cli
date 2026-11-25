// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;

/**
 * The store of branches on their table, through the repository, whose SQL follows the spec. Every
 * call joins the transaction of the use case that makes it, and a failure of the database comes out
 * of it as a {@link Transactions.DatabaseException}: the store knows no JDBC.
 */
public class BranchPersistenceAdapter implements BranchStore {

  private final Transactions transactions;
  private final BranchRepository repository;

  public BranchPersistenceAdapter(Transactions transactions, BranchRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @Override
  public List<Branch> findAll() {
    return transactions.run(repository::findAll);
  }

  @Override
  public Optional<Branch> findById(Long id) {
    return transactions.run(() -> repository.findById(id));
  }

  @Override
  public Branch insert(Branch branch) {
    return transactions.run(() -> repository.insert(branch));
  }

  @Override
  public boolean update(Branch branch) {
    return transactions.run(() -> repository.update(branch));
  }

  @Override
  public boolean delete(Long id) {
    return transactions.run(() -> repository.delete(id));
  }
}
