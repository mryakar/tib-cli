// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

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

/** The table of items, against PostgreSQL in a container. */
class ItemRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final ItemRepository itemRepository = new ItemRepository(transactions);
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
    Item item =
        transactions.run(() -> itemRepository.insert(ItemSamples.item(branch.getId()).build()));

    Item found = transactions.run(() -> itemRepository.findById(item.getId())).orElseThrow();
    assertEquals(item.getLabel(), found.getLabel());
    assertEquals(item.getItem(), found.getItem());
    assertEquals(branch.getId(), found.getBranchId());
  }

  @Test
  void updatesAnItem() {
    Item item =
        transactions.run(() -> itemRepository.insert(ItemSamples.item(branch.getId()).build()));
    Item changed = ItemSamples.otherItem(branch.getId()).build();
    changed.setId(item.getId());

    assertTrue(transactions.run(() -> itemRepository.update(changed)));
    Item found = transactions.run(() -> itemRepository.findById(item.getId())).orElseThrow();
    assertEquals(changed.getLabel(), found.getLabel());
    assertEquals(changed.getItem(), found.getItem());
  }

  @Test
  void listsItems() {
    Item item =
        transactions.run(() -> itemRepository.insert(ItemSamples.item(branch.getId()).build()));

    assertEquals(
        item.getId(),
        transactions.run(() -> itemRepository.findAll(0, 20)).content().getFirst().getId());
    assertEquals(1, transactions.run(itemRepository::count));
  }

  @Test
  void deletesAnItem() {
    Item item =
        transactions.run(() -> itemRepository.insert(ItemSamples.item(branch.getId()).build()));

    assertTrue(transactions.run(() -> itemRepository.delete(item.getId())));
    assertFalse(transactions.run(() -> itemRepository.findById(item.getId())).isPresent());
  }
}
