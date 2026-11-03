// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/** The table of notes, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class NoteRepositoryTests {

  @Autowired private NoteRepository notes;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsANote() {
    Note note = notes.saveAndFlush(newNote());
    entityManager.clear();

    Note found = notes.findById(note.getId()).orElseThrow();
    assertThat(found.getText()).isEqualTo("One");
  }

  private Note newNote() {
    Note note = new Note();
    note.setText("One");
    return note;
  }
}
