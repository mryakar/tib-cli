// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag.domain;

/**
 * The values of {@link Tag}, as the spec names them, in plain Java: the domain knows no framework.
 */
public abstract class TagGenerated {

  private Long id;
  private String label;
  private Long branchId;
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
