// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.plain.PlainSamples;
import me.yakar.cornerservice.plain.domain.Plain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The store of plains on their table, against PostgreSQL in a container. */
class PlainPersistenceAdapterTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final PlainRepository plainRepository = new PlainRepository(transactions);
  private final PlainPersistenceAdapter store =
      new PlainPersistenceAdapter(transactions, plainRepository);

  @AfterEach
  void deleteAll() {
    TestDatabase.clear("PLAIN");
  }

  @Test
  void savesAndFindsAPlain() {
    Plain plain = store.insert(PlainSamples.plain().build());

    Plain found = store.findById(plain.getId()).orElseThrow();
    assertEquals(plain.getLabel(), found.getLabel());
  }

  @Test
  void updatesAPlain() {
    Plain plain = store.insert(PlainSamples.plain().build());
    Plain changed = PlainSamples.otherPlain().build();
    changed.setId(plain.getId());

    assertTrue(store.update(changed));
    Plain found = store.findById(plain.getId()).orElseThrow();
    assertEquals(changed.getLabel(), found.getLabel());
  }

  @Test
  void listsPlains() {
    Plain plain = store.insert(PlainSamples.plain().build());

    assertEquals(plain.getId(), store.findAll(0, 20).content().getFirst().getId());
    assertEquals(1, store.findAll(0, 20).page().totalElements());
  }

  @Test
  void deletesAPlain() {
    Plain plain = store.insert(PlainSamples.plain().build());

    assertTrue(store.delete(plain.getId()));
    assertFalse(store.findById(plain.getId()).isPresent());
  }
}
