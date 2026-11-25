// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.note.NoteSamples;
import me.yakar.cornerservice.note.application.port.out.NoteStore;
import me.yakar.cornerservice.note.domain.Note;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

/** The store of notes on their table, through its adapter, against PostgreSQL in a container. */
@DataJpaTest
@Import({
  TestcontainersConfiguration.class,
  NotePersistenceAdapter.class,
  NotePersistenceMapper.class
})
class NotePersistenceAdapterTests {

  @Autowired private NoteStore notes;
  @Autowired private TestEntityManager entityManager;

  @Test
  void savesAndFindsANote() {
    Note note = notes.save(NoteSamples.note().build());
    entityManager.clear();

    Note found = notes.findById(note.getId()).orElseThrow();
    assertThat(found.getText()).isEqualTo(note.getText());
    assertThat(found.getBody()).isEqualTo(note.getBody());
  }

  @Test
  void listsNotes() {
    Note note = notes.save(NoteSamples.note().build());

    assertThat(notes.findAll(Pageable.unpaged())).extracting(Note::getId).contains(note.getId());
  }

  @Test
  void deletesANote() {
    Note note = notes.save(NoteSamples.note().build());

    notes.delete(note);
    entityManager.flush();
    entityManager.clear();

    assertThat(notes.findById(note.getId())).isEmpty();
  }
}
