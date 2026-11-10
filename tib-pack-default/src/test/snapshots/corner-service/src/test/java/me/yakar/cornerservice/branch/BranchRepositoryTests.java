// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of branches, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BranchRepositoryTests {

  @Autowired private BranchRepository branches;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsABranch() {
    Branch branch = branches.saveAndFlush(BranchSamples.branch().build());
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
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }
}
