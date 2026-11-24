// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The table of plains, against PostgreSQL in a container. */
class PlainRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final PlainRepository plainRepository = new PlainRepository(transactions);

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          plainRepository.deleteAll();
          return null;
        });
  }

  @Test
  void savesAndFindsAPlain() {
    Plain plain = transactions.run(() -> plainRepository.insert(PlainSamples.plain().build()));

    Plain found = transactions.run(() -> plainRepository.findById(plain.getId())).orElseThrow();
    assertEquals(plain.getLabel(), found.getLabel());
  }

  @Test
  void updatesAPlain() {
    Plain plain = transactions.run(() -> plainRepository.insert(PlainSamples.plain().build()));
    Plain changed = PlainSamples.otherPlain().build();
    changed.setId(plain.getId());

    assertTrue(transactions.run(() -> plainRepository.update(changed)));
    Plain found = transactions.run(() -> plainRepository.findById(plain.getId())).orElseThrow();
    assertEquals(changed.getLabel(), found.getLabel());
  }

  @Test
  void listsPlains() {
    Plain plain = transactions.run(() -> plainRepository.insert(PlainSamples.plain().build()));

    assertEquals(
        plain.getId(),
        transactions.run(() -> plainRepository.findAll(0, 20)).content().getFirst().getId());
    assertEquals(1, transactions.run(plainRepository::count));
  }

  @Test
  void deletesAPlain() {
    Plain plain = transactions.run(() -> plainRepository.insert(PlainSamples.plain().build()));

    assertTrue(transactions.run(() -> plainRepository.delete(plain.getId())));
    assertFalse(transactions.run(() -> plainRepository.findById(plain.getId())).isPresent());
  }
}
