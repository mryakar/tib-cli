// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.vehicleservice.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
    Depot depot = depots.saveAndFlush(newDepot());
    entityManager.clear();

    Depot found = depots.findById(depot.getId()).orElseThrow();
    assertThat(found.getCode()).isEqualTo("One");
    assertThat(found.getOpenedOn()).isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void refusesADepotWhoseUniqueValuesAreTaken() {
    depots.saveAndFlush(newDepot());

    assertThatThrownBy(() -> depots.saveAndFlush(newDepot()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Depot newDepot() {
    Depot depot = new Depot();
    depot.setCode("One");
    depot.setOpenedOn(LocalDate.of(2026, 1, 1));
    return depot;
  }
}
