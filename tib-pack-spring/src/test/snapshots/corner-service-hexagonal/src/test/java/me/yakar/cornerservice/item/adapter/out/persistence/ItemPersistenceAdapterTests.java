// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchPersistenceAdapter;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchPersistenceMapper;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.item.ItemSamples;
import me.yakar.cornerservice.item.application.port.out.ItemStore;
import me.yakar.cornerservice.item.domain.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

/** The store of items on their table, through its adapter, against PostgreSQL in a container. */
@DataJpaTest
@Import({
  TestcontainersConfiguration.class,
  ItemPersistenceAdapter.class,
  ItemPersistenceMapper.class,
  BranchPersistenceAdapter.class,
  BranchPersistenceMapper.class
})
class ItemPersistenceAdapterTests {

  @Autowired private ItemStore items;
  @Autowired private BranchStore branches;
  @Autowired private TestEntityManager entityManager;

  private Branch branch;

  @BeforeEach
  void saveWhatAnItemNeeds() {
    branch = branches.save(BranchSamples.branch().build());
  }

  @Test
  void savesAndFindsAnItem() {
    Item item = items.save(ItemSamples.item(branch.getId()).build());
    entityManager.clear();

    Item found = items.findById(item.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo(item.getLabel());
    assertThat(found.getItem()).isEqualTo(item.getItem());
    assertThat(found.getBranchId()).isEqualTo(branch.getId());
  }

  @Test
  void listsItems() {
    Item item = items.save(ItemSamples.item(branch.getId()).build());

    assertThat(items.findAll(Pageable.unpaged())).extracting(Item::getId).contains(item.getId());
  }

  @Test
  void deletesAnItem() {
    Item item = items.save(ItemSamples.item(branch.getId()).build());

    items.delete(item);
    entityManager.flush();
    entityManager.clear();

    assertThat(items.findById(item.getId())).isEmpty();
  }
}
