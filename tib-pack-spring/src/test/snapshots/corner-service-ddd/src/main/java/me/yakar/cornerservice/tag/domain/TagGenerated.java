// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** The columns of TAG, as the spec names them; {@link Tag} is the entity. */
@MappedSuperclass
public abstract class TagGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @Column(name = "LABEL", length = 255)
  private String label;

  @Column(name = "BRANCH_ID", nullable = false)
  private Long branchId;

  @Column(name = "OTHER_ID")
  private Long otherId;

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

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public Long getOtherId() {
    return otherId;
  }

  public void setOtherId(Long otherId) {
    this.otherId = otherId;
  }
}
