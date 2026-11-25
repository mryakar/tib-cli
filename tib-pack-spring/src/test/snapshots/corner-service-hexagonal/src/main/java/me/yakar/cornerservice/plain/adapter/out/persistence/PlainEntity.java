// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.plain.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The table PLAIN, its columns as the spec names them; {@link PlainPersistenceMapper} maps it to
 * and from the domain.
 */
@Entity
@Table(name = "PLAIN")
public class PlainEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "LABEL", length = 255)
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
