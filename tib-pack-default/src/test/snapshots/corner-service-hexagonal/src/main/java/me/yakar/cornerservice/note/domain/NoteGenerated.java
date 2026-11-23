// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.note.domain;

/**
 * The values of {@link Note}, as the spec names them, in plain Java: the domain knows no framework.
 */
public abstract class NoteGenerated {

  private Long id;
  private String text;
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
