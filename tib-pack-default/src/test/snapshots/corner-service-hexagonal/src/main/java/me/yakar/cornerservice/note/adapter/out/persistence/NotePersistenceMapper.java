// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.note.adapter.out.persistence;

import me.yakar.cornerservice.note.domain.Note;
import org.springframework.stereotype.Component;

/** Between the table and the domain: every value, the identifier included. */
@Component
public class NotePersistenceMapper {

  public Note toDomain(NoteEntity entity) {
    Note note = new Note();
    note.setId(entity.getId());
    note.setText(entity.getText());
    note.setBody(entity.getBody());
    return note;
  }

  public NoteEntity toEntity(Note note) {
    NoteEntity entity = new NoteEntity();
    entity.setId(note.getId());
    entity.setText(note.getText());
    entity.setBody(note.getBody());
    return entity;
  }
}
