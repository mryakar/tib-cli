// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.vehicleservice.TestDatabase;
import com.example.vehicleservice.Transactions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The table of vehicles, against PostgreSQL in a container. */
class VehicleRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final VehicleRepository vehicleRepository = new VehicleRepository(transactions);

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          vehicleRepository.deleteAll();
          return null;
        });
  }

  @Test
  void savesAndFindsAVehicle() {
    Vehicle vehicle =
        transactions.run(() -> vehicleRepository.insert(VehicleSamples.vehicle().build()));

    Vehicle found =
        transactions.run(() -> vehicleRepository.findById(vehicle.getId())).orElseThrow();
    assertEquals(vehicle.getPlateNumber(), found.getPlateNumber());
    assertEquals(vehicle.getCapacity(), found.getCapacity());
    assertEquals(vehicle.getPrice(), found.getPrice());
    assertNotNull(found.getCreatedAt());
    assertEquals(vehicle.getUpdatedAt(), found.getUpdatedAt());
  }

  @Test
  void updatesAVehicle() {
    Vehicle vehicle =
        transactions.run(() -> vehicleRepository.insert(VehicleSamples.vehicle().build()));
    Vehicle changed = VehicleSamples.otherVehicle().build();
    changed.setId(vehicle.getId());

    assertTrue(transactions.run(() -> vehicleRepository.update(changed)));
    Vehicle found =
        transactions.run(() -> vehicleRepository.findById(vehicle.getId())).orElseThrow();
    assertEquals(changed.getPlateNumber(), found.getPlateNumber());
    assertEquals(changed.getCapacity(), found.getCapacity());
    assertEquals(changed.getPrice(), found.getPrice());
    assertEquals(changed.getUpdatedAt(), found.getUpdatedAt());
  }

  @Test
  void listsVehicles() {
    Vehicle vehicle =
        transactions.run(() -> vehicleRepository.insert(VehicleSamples.vehicle().build()));

    assertEquals(
        vehicle.getId(),
        transactions.run(() -> vehicleRepository.findAll(0, 20)).content().getFirst().getId());
    assertEquals(1, transactions.run(vehicleRepository::count));
  }

  @Test
  void deletesAVehicle() {
    Vehicle vehicle =
        transactions.run(() -> vehicleRepository.insert(VehicleSamples.vehicle().build()));

    assertTrue(transactions.run(() -> vehicleRepository.delete(vehicle.getId())));
    assertFalse(transactions.run(() -> vehicleRepository.findById(vehicle.getId())).isPresent());
  }

  @Test
  void refusesAVehicleWhoseUniqueValuesAreTaken() {
    transactions.run(() -> vehicleRepository.insert(VehicleSamples.vehicle().build()));

    Transactions.DatabaseException e =
        assertThrows(
            Transactions.DatabaseException.class,
            () ->
                transactions.run(() -> vehicleRepository.insert(VehicleSamples.vehicle().build())));
    assertTrue(e.getCause().getSQLState().startsWith("23"));
  }
}
