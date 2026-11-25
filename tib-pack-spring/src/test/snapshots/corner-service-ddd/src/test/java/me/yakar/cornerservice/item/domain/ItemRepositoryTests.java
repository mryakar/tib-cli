// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.branch.domain.BranchRepository;
import me.yakar.cornerservice.item.ItemSamples;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of items, against PostgreSQL in a container. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ItemRepositoryTests {

  @Autowired private ItemRepository items;
  @Autowired private BranchRepository branches;
  @Autowired private TestEntityManager entityManager;

  private Branch branch;

  @BeforeEach
  void saveWhatAnItemNeeds() {
    branch = branches.saveAndFlush(BranchSamples.branch().build());
  }

  @Test
  void savesAndFindsAnItem() {
    Item item = items.saveAndFlush(ItemSamples.item(branch.getId()).build());
    entityManager.clear();

    Item found = items.findById(item.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo(item.getLabel());
    assertThat(found.getItem()).isEqualTo(item.getItem());
    assertThat(found.getBranchId()).isEqualTo(branch.getId());
  }
}
