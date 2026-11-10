// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.vehicleservice.TestcontainersConfiguration;
import java.math.BigDecimal;
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
    Vehicle vehicle = vehicles.saveAndFlush(newVehicle());
    entityManager.clear();

    Vehicle found = vehicles.findById(vehicle.getId()).orElseThrow();
    assertThat(found.getPlateNumber()).isEqualTo("One");
    assertThat(found.getCapacity()).isEqualTo(1);
    assertThat(found.getPrice()).isEqualTo(new BigDecimal("1.00"));
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  void refusesAVehicleWhoseUniqueValuesAreTaken() {
    vehicles.saveAndFlush(newVehicle());

    assertThatThrownBy(() -> vehicles.saveAndFlush(newVehicle()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Vehicle newVehicle() {
    Vehicle vehicle = new Vehicle();
    vehicle.setPlateNumber("One");
    vehicle.setCapacity(1);
    vehicle.setPrice(new BigDecimal("1.00"));
    return vehicle;
  }
}
