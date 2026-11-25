// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;
import org.springframework.stereotype.Component;

/**
 * The store of branches on their table, through JPA; the mapper maps them to and from the domain.
 */
@Component
public class BranchPersistenceAdapter implements BranchStore {

  private final BranchJpaRepository branches;
  private final BranchPersistenceMapper mapper;

  public BranchPersistenceAdapter(BranchJpaRepository branches, BranchPersistenceMapper mapper) {
    this.branches = branches;
    this.mapper = mapper;
  }

  @Override
  public List<Branch> findAll() {
    return branches.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Optional<Branch> findById(Long id) {
    return branches.findById(id).map(mapper::toDomain);
  }

  /** Saves the branch and flushes it, so that a broken constraint shows here. */
  @Override
  public Branch save(Branch branch) {
    return mapper.toDomain(branches.saveAndFlush(mapper.toEntity(branch)));
  }

  @Override
  public void delete(Branch branch) {
    branches.deleteById(branch.getId());
  }
}
