// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.vehicleservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** The table of vehicles, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class VehicleRepositoryTests {

  @Autowired private VehicleRepository vehicles;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsAVehicle() {
    Vehicle vehicle = vehicles.saveAndFlush(VehicleSamples.vehicle().build());
    entityManager.clear();

    Vehicle found = vehicles.findById(vehicle.getId()).orElseThrow();
    assertThat(found.getPlateNumber()).isEqualTo(vehicle.getPlateNumber());
    assertThat(found.getCapacity()).isEqualTo(vehicle.getCapacity());
    assertThat(found.getPrice()).isEqualTo(vehicle.getPrice());
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  void refusesAVehicleWhoseUniqueValuesAreTaken() {
    vehicles.saveAndFlush(VehicleSamples.vehicle().build());

    assertThatThrownBy(() -> vehicles.saveAndFlush(VehicleSamples.vehicle().build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
