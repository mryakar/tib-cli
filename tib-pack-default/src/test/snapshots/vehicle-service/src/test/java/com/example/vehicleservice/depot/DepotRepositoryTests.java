// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

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

/** The table of depots, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DepotRepositoryTests {

  @Autowired private DepotRepository depots;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsADepot() {
    Depot depot = depots.saveAndFlush(DepotSamples.depot().build());
    entityManager.clear();

    Depot found = depots.findById(depot.getId()).orElseThrow();
    assertThat(found.getCode()).isEqualTo(depot.getCode());
    assertThat(found.getOpenedOn()).isEqualTo(depot.getOpenedOn());
  }

  @Test
  void refusesADepotWhoseUniqueValuesAreTaken() {
    depots.saveAndFlush(DepotSamples.depot().build());

    assertThatThrownBy(() -> depots.saveAndFlush(DepotSamples.depot().build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
