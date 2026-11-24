// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item.domain;

/**
 * The columns of ITEM, as the spec names them, in plain Java; {@link Item} is the entity, and a
 * relation is the identifier of its target.
 */
public abstract class ItemGenerated {

  private Long id;
  private String label;
  private String item;
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
