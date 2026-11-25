// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {}
