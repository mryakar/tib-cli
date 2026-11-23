// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.note.application.port.out.NoteStore;
import me.yakar.cornerservice.note.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** The store of notes on their table, through JPA; the mapper maps them to and from the domain. */
@Component
public class NotePersistenceAdapter implements NoteStore {

  private final NoteJpaRepository notes;
  private final NotePersistenceMapper mapper;

  public NotePersistenceAdapter(NoteJpaRepository notes, NotePersistenceMapper mapper) {
    this.notes = notes;
    this.mapper = mapper;
  }

  @Override
  public Page<Note> findAll(Pageable pageable) {
    return notes.findAll(pageable).map(mapper::toDomain);
  }

  @Override
  public Optional<Note> findById(Long id) {
    return notes.findById(id).map(mapper::toDomain);
  }

  /** Saves the note and flushes it, so that a broken constraint shows here. */
  @Override
  public Note save(Note note) {
    return mapper.toDomain(notes.saveAndFlush(mapper.toEntity(note)));
  }

  @Override
  public void delete(Note note) {
    notes.deleteById(note.getId());
  }
}
