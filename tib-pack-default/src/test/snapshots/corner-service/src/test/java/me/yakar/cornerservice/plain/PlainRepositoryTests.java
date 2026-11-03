// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
    Plain plain = plains.saveAndFlush(newPlain());
    entityManager.clear();

    Plain found = plains.findById(plain.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo("One");
  }

  private Plain newPlain() {
    Plain plain = new Plain();
    plain.setLabel("One");
    return plain;
  }
}
