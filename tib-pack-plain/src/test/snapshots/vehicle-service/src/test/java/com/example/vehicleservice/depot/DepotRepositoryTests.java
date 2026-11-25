// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.vehicleservice.TestDatabase;
import com.example.vehicleservice.Transactions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The table of depots, against PostgreSQL in a container. */
class DepotRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final DepotRepository depotRepository = new DepotRepository(transactions);

  @AfterEach
  void deleteAll() {
    TestDatabase.clear("DEPOTS");
  }

  @Test
  void savesAndFindsADepot() {
    Depot depot = transactions.run(() -> depotRepository.insert(DepotSamples.depot().build()));

    Depot found = transactions.run(() -> depotRepository.findById(depot.getId())).orElseThrow();
    assertEquals(depot.getCode(), found.getCode());
    assertEquals(depot.getOpenedOn(), found.getOpenedOn());
  }

  @Test
  void updatesADepot() {
    Depot depot = transactions.run(() -> depotRepository.insert(DepotSamples.depot().build()));
    Depot changed = DepotSamples.otherDepot().build();
    changed.setId(depot.getId());

    assertTrue(transactions.run(() -> depotRepository.update(changed)));
    Depot found = transactions.run(() -> depotRepository.findById(depot.getId())).orElseThrow();
    assertEquals(changed.getCode(), found.getCode());
    assertEquals(changed.getOpenedOn(), found.getOpenedOn());
  }

  @Test
  void listsDepots() {
    Depot depot = transactions.run(() -> depotRepository.insert(DepotSamples.depot().build()));

    assertEquals(
        depot.getId(),
        transactions.run(() -> depotRepository.findAll(0, 20)).content().getFirst().getId());
    assertEquals(1, transactions.run(depotRepository::count));
  }

  @Test
  void deletesADepot() {
    Depot depot = transactions.run(() -> depotRepository.insert(DepotSamples.depot().build()));

    assertTrue(transactions.run(() -> depotRepository.delete(depot.getId())));
    assertFalse(transactions.run(() -> depotRepository.findById(depot.getId())).isPresent());
  }

  @Test
  void refusesADepotWhoseUniqueValuesAreTaken() {
    transactions.run(() -> depotRepository.insert(DepotSamples.depot().build()));

    Transactions.DatabaseException e =
        assertThrows(
            Transactions.DatabaseException.class,
            () -> transactions.run(() -> depotRepository.insert(DepotSamples.depot().build())));
    assertTrue(e.getCause().getSQLState().startsWith("23"));
  }
}
