// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.domain;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.branch.domain.BranchRepository;
import me.yakar.cornerservice.tag.TagSamples;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of tags, against PostgreSQL in a container. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class TagRepositoryTests {

  @Autowired private TagRepository tags;
  @Autowired private BranchRepository branches;
  @Autowired private TestEntityManager entityManager;

  private Branch branch;

  @BeforeEach
  void saveWhatATagNeeds() {
    branch = branches.saveAndFlush(BranchSamples.branch().build());
  }

  @Test
  void savesAndFindsATag() {
    Tag tag = tags.saveAndFlush(TagSamples.tag(branch.getId()).build());
    entityManager.clear();

    Tag found = tags.findById(tag.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo(tag.getLabel());
    assertThat(found.getBranchId()).isEqualTo(branch.getId());
  }
}
