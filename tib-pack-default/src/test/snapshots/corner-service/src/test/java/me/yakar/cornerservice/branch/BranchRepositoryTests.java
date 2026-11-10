// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
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
    Branch branch = branches.saveAndFlush(newBranch());
    entityManager.clear();

    Branch found = branches.findById(branch.getId()).orElseThrow();
    assertThat(found.getCode()).isEqualTo("One");
    assertThat(found.getCount()).isEqualTo(1L);
    assertThat(found.getOpen()).isEqualTo(true);
    assertThat(found.getOpenedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
    assertThat(found.getSeenAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
    assertThat(found.getToken()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    assertThat(found.getRatio()).isEqualTo(new BigDecimal("1"));
    assertThat(found.getAmount()).isEqualTo(new BigDecimal("1"));
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  private Branch newBranch() {
    Branch branch = new Branch();
    branch.setCode("One");
    branch.setCount(1L);
    branch.setOpen(true);
    branch.setOpenedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
    branch.setSeenAt(Instant.parse("2026-01-01T12:00:00Z"));
    branch.setToken(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    branch.setRatio(new BigDecimal("1"));
    branch.setAmount(new BigDecimal("1"));
    return branch;
  }
}
