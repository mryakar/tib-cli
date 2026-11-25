// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.note.NoteSamples;
import me.yakar.cornerservice.note.domain.Note;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The store of notes on their table, against PostgreSQL in a container. */
class NotePersistenceAdapterTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final NoteRepository noteRepository = new NoteRepository(transactions);
  private final NotePersistenceAdapter store =
      new NotePersistenceAdapter(transactions, noteRepository);

  @AfterEach
  void deleteAll() {
    TestDatabase.clear("NOTE");
  }

  @Test
  void savesAndFindsANote() {
    Note note = store.insert(NoteSamples.note().build());

    Note found = store.findById(note.getId()).orElseThrow();
    assertEquals(note.getText(), found.getText());
    assertEquals(note.getBody(), found.getBody());
  }

  @Test
  void updatesANote() {
    Note note = store.insert(NoteSamples.note().build());
    Note changed = NoteSamples.otherNote().build();
    changed.setId(note.getId());

    assertTrue(store.update(changed));
    Note found = store.findById(note.getId()).orElseThrow();
    assertEquals(changed.getText(), found.getText());
    assertEquals(changed.getBody(), found.getBody());
  }

  @Test
  void listsNotes() {
    Note note = store.insert(NoteSamples.note().build());

    assertEquals(note.getId(), store.findAll(0, 20).content().getFirst().getId());
    assertEquals(1, store.findAll(0, 20).page().totalElements());
  }

  @Test
  void deletesANote() {
    Note note = store.insert(NoteSamples.note().build());

    assertTrue(store.delete(note.getId()));
    assertFalse(store.findById(note.getId()).isPresent());
  }
}
