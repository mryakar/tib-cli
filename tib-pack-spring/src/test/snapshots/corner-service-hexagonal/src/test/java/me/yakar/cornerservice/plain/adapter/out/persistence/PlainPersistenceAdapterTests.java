// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.plain.PlainSamples;
import me.yakar.cornerservice.plain.application.port.out.PlainStore;
import me.yakar.cornerservice.plain.domain.Plain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

/** The store of plains on their table, through its adapter, against PostgreSQL in a container. */
@DataJpaTest
@Import({
  TestcontainersConfiguration.class,
  PlainPersistenceAdapter.class,
  PlainPersistenceMapper.class
})
class PlainPersistenceAdapterTests {

  @Autowired private PlainStore plains;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsAPlain() {
    Plain plain = plains.save(PlainSamples.plain().build());
    entityManager.clear();

    Plain found = plains.findById(plain.getId()).orElseThrow();
    assertThat(found.getLabel()).isEqualTo(plain.getLabel());
  }

  @Test
  void listsPlains() {
    Plain plain = plains.save(PlainSamples.plain().build());

    assertThat(plains.findAll(Pageable.unpaged())).extracting(Plain::getId).contains(plain.getId());
  }

  @Test
  void deletesAPlain() {
    Plain plain = plains.save(PlainSamples.plain().build());

    plains.delete(plain);
    entityManager.flush();
    entityManager.clear();

    assertThat(plains.findById(plain.getId())).isEmpty();
  }
}
