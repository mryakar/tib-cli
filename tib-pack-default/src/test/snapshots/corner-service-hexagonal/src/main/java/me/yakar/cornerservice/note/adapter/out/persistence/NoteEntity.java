// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.note.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The table NOTE, its columns as the spec names them; {@link NotePersistenceMapper} maps it to and
 * from the domain.
 */
@Entity
@Table(name = "NOTE")
public class NoteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "TEXT", length = 2000)
  private String text;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "BODY", nullable = false)
  private String body;

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

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
