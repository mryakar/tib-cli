// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The table of branches, against PostgreSQL in a container. */
class BranchRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final BranchRepository branchRepository = new BranchRepository(transactions);

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
    Branch branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));

    Branch found = transactions.run(() -> branchRepository.findById(branch.getId())).orElseThrow();
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
    Branch branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));
    Branch changed = BranchSamples.otherBranch().build();
    changed.setId(branch.getId());

    assertTrue(transactions.run(() -> branchRepository.update(changed)));
    Branch found = transactions.run(() -> branchRepository.findById(branch.getId())).orElseThrow();
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
    Branch branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));

    assertEquals(
        branch.getId(), transactions.run(() -> branchRepository.findAll()).getFirst().getId());
    assertEquals(1, transactions.run(branchRepository::count));
  }

  @Test
  void deletesABranch() {
    Branch branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));

    assertTrue(transactions.run(() -> branchRepository.delete(branch.getId())));
    assertFalse(transactions.run(() -> branchRepository.findById(branch.getId())).isPresent());
  }
}
