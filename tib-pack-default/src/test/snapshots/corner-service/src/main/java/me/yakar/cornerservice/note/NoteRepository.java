// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.note;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {}
