// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteJpaRepository extends JpaRepository<NoteEntity, Long> {}
