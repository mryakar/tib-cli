// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The store of branches on their table, through its adapter, against PostgreSQL in a container. */
@DataJpaTest
@Import({
  TestcontainersConfiguration.class,
  BranchPersistenceAdapter.class,
  BranchPersistenceMapper.class
})
class BranchPersistenceAdapterTests {

  @Autowired private BranchStore branches;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsABranch() {
    Branch branch = branches.save(BranchSamples.branch().build());
    entityManager.clear();

    Branch found = branches.findById(branch.getId()).orElseThrow();
    assertThat(found.getCode()).isEqualTo(branch.getCode());
    assertThat(found.getCount()).isEqualTo(branch.getCount());
    assertThat(found.getOpen()).isEqualTo(branch.getOpen());
    assertThat(found.getOpenedAt()).isEqualTo(branch.getOpenedAt());
    assertThat(found.getSeenAt()).isEqualTo(branch.getSeenAt());
    assertThat(found.getToken()).isEqualTo(branch.getToken());
    assertThat(found.getRatio()).isEqualTo(branch.getRatio());
    assertThat(found.getAmount()).isEqualTo(branch.getAmount());
    assertThat(found.getMemo()).isEqualTo(branch.getMemo());
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  void listsBranches() {
    Branch branch = branches.save(BranchSamples.branch().build());

    assertThat(branches.findAll()).extracting(Branch::getId).contains(branch.getId());
  }

  @Test
  void deletesABranch() {
    Branch branch = branches.save(BranchSamples.branch().build());

    branches.delete(branch);
    entityManager.flush();
    entityManager.clear();

    assertThat(branches.findById(branch.getId())).isEmpty();
  }
}
