// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.Branch;
import me.yakar.cornerservice.branch.BranchRepository;
import me.yakar.cornerservice.branch.BranchSamples;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The table of tags, against PostgreSQL in a container. */
class TagRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final TagRepository tagRepository = new TagRepository(transactions);
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
    Tag tag = transactions.run(() -> tagRepository.insert(TagSamples.tag(branch.getId()).build()));

    Tag found = transactions.run(() -> tagRepository.findById(tag.getId())).orElseThrow();
    assertEquals(tag.getLabel(), found.getLabel());
    assertEquals(branch.getId(), found.getBranchId());
  }

  @Test
  void updatesATag() {
    Tag tag = transactions.run(() -> tagRepository.insert(TagSamples.tag(branch.getId()).build()));
    Tag changed = TagSamples.otherTag(branch.getId()).build();
    changed.setId(tag.getId());

    assertTrue(transactions.run(() -> tagRepository.update(changed)));
    Tag found = transactions.run(() -> tagRepository.findById(tag.getId())).orElseThrow();
    assertEquals(changed.getLabel(), found.getLabel());
  }

  @Test
  void listsTags() {
    Tag tag = transactions.run(() -> tagRepository.insert(TagSamples.tag(branch.getId()).build()));

    assertEquals(
        tag.getId(),
        transactions.run(() -> tagRepository.findAll(0, 20)).content().getFirst().getId());
    assertEquals(1, transactions.run(tagRepository::count));
  }

  @Test
  void deletesATag() {
    Tag tag = transactions.run(() -> tagRepository.insert(TagSamples.tag(branch.getId()).build()));

    assertTrue(transactions.run(() -> tagRepository.delete(tag.getId())));
    assertFalse(transactions.run(() -> tagRepository.findById(tag.getId())).isPresent());
  }
}
