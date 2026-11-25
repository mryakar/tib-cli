// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The table ITEM, its columns as the spec names them; {@link ItemPersistenceMapper} maps it to and
 * from the domain.
 */
@Entity
@Table(name = "ITEM")
public class ItemEntity {

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
