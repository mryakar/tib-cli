// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** The columns of ITEM, as the spec names them; {@link Item} is the entity. */
@MappedSuperclass
public abstract class ItemGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "LABEL", length = 255)
  private String label;

  @Column(name = "ITEM", length = 255)
  private String item;

  @Column(name = "BRANCH_ID", nullable = false)
  private Long branchId;

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

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }
}
