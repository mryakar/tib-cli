// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchRepository;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.tag.TagSamples;
import me.yakar.cornerservice.tag.domain.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The store of tags on their table, against PostgreSQL in a container. */
class TagPersistenceAdapterTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final TagRepository tagRepository = new TagRepository(transactions);
  private final TagPersistenceAdapter store =
      new TagPersistenceAdapter(transactions, tagRepository);
  private final BranchRepository branchRepository = new BranchRepository(transactions);

  private Branch branch;

  @BeforeEach
  void saveWhatATagNeeds() {
    branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));
  }

  @AfterEach
  void deleteAll() {
    TestDatabase.clear("TAG", "BRANCH");
  }

  @Test
  void savesAndFindsATag() {
    Tag tag = store.insert(TagSamples.tag(branch.getId()).build());

    Tag found = store.findById(tag.getId()).orElseThrow();
    assertEquals(tag.getLabel(), found.getLabel());
    assertEquals(branch.getId(), found.getBranchId());
  }

  @Test
  void updatesATag() {
    Tag tag = store.insert(TagSamples.tag(branch.getId()).build());
    Tag changed = TagSamples.otherTag(branch.getId()).build();
    changed.setId(tag.getId());

    assertTrue(store.update(changed));
    Tag found = store.findById(tag.getId()).orElseThrow();
    assertEquals(changed.getLabel(), found.getLabel());
  }

  @Test
  void listsTags() {
    Tag tag = store.insert(TagSamples.tag(branch.getId()).build());

    assertEquals(tag.getId(), store.findAll(0, 20).content().getFirst().getId());
    assertEquals(1, store.findAll(0, 20).page().totalElements());
  }

  @Test
  void deletesATag() {
    Tag tag = store.insert(TagSamples.tag(branch.getId()).build());

    assertTrue(store.delete(tag.getId()));
    assertFalse(store.findById(tag.getId()).isPresent());
  }
}
