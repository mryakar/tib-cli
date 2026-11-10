// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.Branch;
import me.yakar.cornerservice.branch.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of tags, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TagRepositoryTests {

  @Autowired private TagRepository tags;
  @Autowired private BranchRepository branches;
  @Autowired private TestEntityManager entityManager;

  private Branch branch;

  @BeforeEach
  void saveWhatATagNeeds() {
    branch = branches.saveAndFlush(newBranch());
  }

  @Test
  void savesAndFindsATag() {
    Tag tag = tags.saveAndFlush(newTag());
    entityManager.clear();

    Tag found = tags.findById(tag.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo("One");
    assertThat(found.getBranch().getId()).isEqualTo(branch.getId());
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

  private Tag newTag() {
    Tag tag = new Tag();
    tag.setLabel("One");
    tag.setBranch(branch);
    return tag;
  }
}
