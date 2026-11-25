// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import java.sql.SQLException;
import java.util.List;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;

/** The use cases of branches, each in a transaction, on their DTOs. */
public class BranchService {

  private final Transactions transactions;
  private final BranchRepository repository;
  private final BranchMapper mapper;

  public BranchService(
      Transactions transactions, BranchRepository repository, BranchMapper mapper) {
    this.transactions = transactions;
    this.repository = repository;
    this.mapper = mapper;
  }

  public List<BranchDto> list() {
    return transactions.run(() -> repository.findAll().stream().map(mapper::toDto).toList());
  }

  public BranchDto get(Long id) {
    return transactions.run(() -> mapper.toDto(find(id)));
  }

  public BranchDto create(BranchDto dto) {
    return transactions.run(
        () -> {
          Branch branch = new Branch();
          mapper.update(dto, branch);
          return mapper.toDto(repository.insert(branch));
        });
  }

  public BranchDto update(Long id, BranchDto dto) {
    return transactions.run(
        () -> {
          Branch branch = find(id);
          mapper.update(dto, branch);
          if (!repository.update(branch)) {
            throw notFound(id);
          }
          return mapper.toDto(branch);
        });
  }

  public void delete(Long id) {
    transactions.run(
        () -> {
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
