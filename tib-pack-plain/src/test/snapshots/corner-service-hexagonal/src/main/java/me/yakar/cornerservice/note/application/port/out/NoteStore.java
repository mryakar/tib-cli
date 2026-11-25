// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.note.domain.Note;

/** Where notes are kept, as the application sees it: an outbound adapter implements it. */
public interface NoteStore {

  /** The page of notes with the number, from 0, of the size. */
  Page<Note> findAll(int page, int size);

  Optional<Note> findById(Long id);

  /** Inserts the note, and gives it back as it was inserted, with its identifier. */
  Note insert(Note note);

  /** Updates the note, and says whether it is there. */
  boolean update(Note note);

  /** Deletes the note with the identifier, and says whether it was there. */
  boolean delete(Long id);
}
