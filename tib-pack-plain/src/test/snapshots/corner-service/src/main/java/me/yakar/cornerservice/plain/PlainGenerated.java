// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.plain;

/** The columns of PLAIN, as the spec names them, in plain Java; {@link Plain} is the entity. */
public abstract class PlainGenerated {

  private Long id;
  private String label;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}
