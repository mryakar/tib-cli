// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.note.NoteSamples;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The table of notes, against PostgreSQL in a container. */
class NoteRepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final NoteRepository noteRepository = new NoteRepository(transactions);

  @AfterEach
  void deleteAll() {
    TestDatabase.clear("NOTE");
  }

  @Test
  void savesAndFindsANote() {
    Note note = transactions.run(() -> noteRepository.insert(NoteSamples.note().build()));

    Note found = transactions.run(() -> noteRepository.findById(note.getId())).orElseThrow();
    assertEquals(note.getText(), found.getText());
    assertEquals(note.getBody(), found.getBody());
  }

  @Test
  void updatesANote() {
    Note note = transactions.run(() -> noteRepository.insert(NoteSamples.note().build()));
    Note changed = NoteSamples.otherNote().build();
    changed.setId(note.getId());

    assertTrue(transactions.run(() -> noteRepository.update(changed)));
    Note found = transactions.run(() -> noteRepository.findById(note.getId())).orElseThrow();
    assertEquals(changed.getText(), found.getText());
    assertEquals(changed.getBody(), found.getBody());
  }

  @Test
  void listsNotes() {
    Note note = transactions.run(() -> noteRepository.insert(NoteSamples.note().build()));

    assertEquals(
        note.getId(),
        transactions.run(() -> noteRepository.findAll(0, 20)).content().getFirst().getId());
    assertEquals(1, transactions.run(noteRepository::count));
  }

  @Test
  void deletesANote() {
    Note note = transactions.run(() -> noteRepository.insert(NoteSamples.note().build()));

    assertTrue(transactions.run(() -> noteRepository.delete(note.getId())));
    assertFalse(transactions.run(() -> noteRepository.findById(note.getId())).isPresent());
  }
}
