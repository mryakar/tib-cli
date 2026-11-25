// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.domain;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.note.NoteSamples;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of notes, against PostgreSQL in a container. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class NoteRepositoryTests {

  @Autowired private NoteRepository notes;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsANote() {
    Note note = notes.saveAndFlush(NoteSamples.note().build());
    entityManager.clear();

    Note found = notes.findById(note.getId()).orElseThrow();
    assertThat(found.getText()).isEqualTo(note.getText());
    assertThat(found.getBody()).isEqualTo(note.getBody());
  }
}
