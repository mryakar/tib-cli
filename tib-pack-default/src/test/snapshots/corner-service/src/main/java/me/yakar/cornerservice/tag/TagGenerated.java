// Written by tib on every run. Do not edit: your changes would be lost. Your code goes in Tag.
package me.yakar.cornerservice.tag;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import me.yakar.cornerservice.branch.Branch;

/** The columns of TAG, as the spec names them; {@link Tag} is the entity. */
@MappedSuperclass
public abstract class TagGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "LABEL")
  private String label;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "BRANCH_ID", nullable = false)
  private Branch branch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OTHER_ID")
  private Branch other;

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

  public Branch getOther() {
    return other;
  }

  public void setOther(Branch other) {
    this.other = other;
  }
}
