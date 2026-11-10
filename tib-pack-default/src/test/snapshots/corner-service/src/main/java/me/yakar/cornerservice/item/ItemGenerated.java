// Written by tib on every run. Do not edit: your changes would be lost. Your code goes in Item.
package me.yakar.cornerservice.item;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import me.yakar.cornerservice.branch.Branch;

/** The columns of ITEM, as the spec names them; {@link Item} is the entity. */
@MappedSuperclass
public abstract class ItemGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "LABEL", length = 255)
  private String label;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "BRANCH_ID", nullable = false)
  private Branch branch;

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

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }
}
