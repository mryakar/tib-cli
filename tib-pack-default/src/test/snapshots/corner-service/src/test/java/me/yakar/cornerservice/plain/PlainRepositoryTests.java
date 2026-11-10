// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of plains, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PlainRepositoryTests {

  @Autowired private PlainRepository plains;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsAPlain() {
    Plain plain = plains.saveAndFlush(PlainSamples.plain().build());
    entityManager.clear();

    Plain found = plains.findById(plain.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo(plain.getLabel());
  }
}
