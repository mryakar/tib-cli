// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A note. The columns are in {@link NoteGenerated}; the behavior goes here. */
@Entity
@Table(name = "NOTE")
public class Note extends NoteGenerated {}
