// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchRepository;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.item.ItemSamples;
import me.yakar.cornerservice.item.domain.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The store of items on their table, against PostgreSQL in a container. */
class ItemPersistenceAdapterTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final ItemRepository itemRepository = new ItemRepository(transactions);
  private final ItemPersistenceAdapter store =
      new ItemPersistenceAdapter(transactions, itemRepository);
  private final BranchRepository branchRepository = new BranchRepository(transactions);

  private Branch branch;

  @BeforeEach
  void saveWhatAnItemNeeds() {
    branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));
  }

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          itemRepository.deleteAll();
          branchRepository.deleteAll();
          return null;
        });
  }

  @Test
  void savesAndFindsAnItem() {
    Item item = store.insert(ItemSamples.item(branch.getId()).build());

    Item found = store.findById(item.getId()).orElseThrow();
    assertEquals(item.getLabel(), found.getLabel());
    assertEquals(item.getItem(), found.getItem());
    assertEquals(branch.getId(), found.getBranchId());
  }

  @Test
  void updatesAnItem() {
    Item item = store.insert(ItemSamples.item(branch.getId()).build());
    Item changed = ItemSamples.otherItem(branch.getId()).build();
    changed.setId(item.getId());

    store.update(changed);
    Item found = store.findById(item.getId()).orElseThrow();
    assertEquals(changed.getLabel(), found.getLabel());
    assertEquals(changed.getItem(), found.getItem());
  }

  @Test
  void listsItems() {
    Item item = store.insert(ItemSamples.item(branch.getId()).build());

    assertEquals(item.getId(), store.findAll(0, 20).content().getFirst().getId());
    assertEquals(1, store.findAll(0, 20).page().totalElements());
  }

  @Test
  void deletesAnItem() {
    Item item = store.insert(ItemSamples.item(branch.getId()).build());

    store.delete(item.getId());
    assertFalse(store.findById(item.getId()).isPresent());
  }
}
