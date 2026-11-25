// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.domain.Branch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The store of branches on their table, against PostgreSQL in a container. */
class BranchPersistenceAdapterTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final BranchRepository branchRepository = new BranchRepository(transactions);
  private final BranchPersistenceAdapter store =
      new BranchPersistenceAdapter(transactions, branchRepository);

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          branchRepository.deleteAll();
          return null;
        });
  }

  @Test
  void savesAndFindsABranch() {
    Branch branch = store.insert(BranchSamples.branch().build());

    Branch found = store.findById(branch.getId()).orElseThrow();
    assertEquals(branch.getCode(), found.getCode());
    assertEquals(branch.getCount(), found.getCount());
    assertEquals(branch.getOpen(), found.getOpen());
    assertEquals(branch.getOpenedAt(), found.getOpenedAt());
    assertEquals(branch.getSeenAt(), found.getSeenAt());
    assertEquals(branch.getToken(), found.getToken());
    assertEquals(branch.getRatio(), found.getRatio());
    assertEquals(branch.getAmount(), found.getAmount());
    assertEquals(branch.getMemo(), found.getMemo());
    assertNotNull(found.getCreatedAt());
    assertEquals(branch.getUpdatedAt(), found.getUpdatedAt());
  }

  @Test
  void updatesABranch() {
    Branch branch = store.insert(BranchSamples.branch().build());
    Branch changed = BranchSamples.otherBranch().build();
    changed.setId(branch.getId());

    store.update(changed);
    Branch found = store.findById(branch.getId()).orElseThrow();
    assertEquals(changed.getCode(), found.getCode());
    assertEquals(changed.getCount(), found.getCount());
    assertEquals(changed.getOpen(), found.getOpen());
    assertEquals(changed.getOpenedAt(), found.getOpenedAt());
    assertEquals(changed.getSeenAt(), found.getSeenAt());
    assertEquals(changed.getToken(), found.getToken());
    assertEquals(changed.getRatio(), found.getRatio());
    assertEquals(changed.getAmount(), found.getAmount());
    assertEquals(changed.getMemo(), found.getMemo());
    assertEquals(changed.getUpdatedAt(), found.getUpdatedAt());
  }

  @Test
  void listsBranches() {
    Branch branch = store.insert(BranchSamples.branch().build());

    assertEquals(branch.getId(), store.findAll().getFirst().getId());
    assertEquals(1, store.findAll().size());
  }

  @Test
  void deletesABranch() {
    Branch branch = store.insert(BranchSamples.branch().build());

    store.delete(branch.getId());
    assertFalse(store.findById(branch.getId()).isPresent());
  }
}
