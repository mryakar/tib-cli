// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchPersistenceAdapter;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchPersistenceMapper;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.tag.TagSamples;
import me.yakar.cornerservice.tag.application.port.out.TagStore;
import me.yakar.cornerservice.tag.domain.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

/** The store of tags on their table, through its adapter, against PostgreSQL in a container. */
@DataJpaTest
@Import({
  TestcontainersConfiguration.class,
  TagPersistenceAdapter.class,
  TagPersistenceMapper.class,
  BranchPersistenceAdapter.class,
  BranchPersistenceMapper.class
})
class TagPersistenceAdapterTests {

  @Autowired private TagStore tags;
  @Autowired private BranchStore branches;
  @Autowired private TestEntityManager entityManager;

  private Branch branch;

  @BeforeEach
  void saveWhatATagNeeds() {
    branch = branches.save(BranchSamples.branch().build());
  }

  @Test
  void savesAndFindsATag() {
    Tag tag = tags.save(TagSamples.tag(branch.getId()).build());
    entityManager.clear();

    Tag found = tags.findById(tag.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo(tag.getLabel());
    assertThat(found.getBranchId()).isEqualTo(branch.getId());
  }

  @Test
  void listsTags() {
    Tag tag = tags.save(TagSamples.tag(branch.getId()).build());

    assertThat(tags.findAll(Pageable.unpaged())).extracting(Tag::getId).contains(tag.getId());
  }

  @Test
  void deletesATag() {
    Tag tag = tags.save(TagSamples.tag(branch.getId()).build());

    tags.delete(tag);
    entityManager.flush();
    entityManager.clear();

    assertThat(tags.findById(tag.getId())).isEmpty();
  }
}
