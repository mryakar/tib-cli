// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.application.port.out;

import java.util.Optional;
import me.yakar.cornerservice.note.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Where notes are kept, as the application sees it: an outbound adapter implements it. */
public interface NoteStore {

  Page<Note> findAll(Pageable pageable);

  Optional<Note> findById(Long id);

  /** Saves the note, and gives it back as it was saved, with its identifier. */
  Note save(Note note);

  void delete(Note note);
}
