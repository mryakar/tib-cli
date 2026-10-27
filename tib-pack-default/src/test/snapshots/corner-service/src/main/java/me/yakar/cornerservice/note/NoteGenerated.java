// Written by tib on every run. Do not edit: your changes would be lost. Your code goes in Note.
package me.yakar.cornerservice.note;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** The columns of NOTE, as the spec names them; {@link Note} is the entity. */
@MappedSuperclass
public abstract class NoteGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "TEXT", length = 2000)
  private String text;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
