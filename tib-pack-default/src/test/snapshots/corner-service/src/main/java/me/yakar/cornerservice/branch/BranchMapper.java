// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch;

import org.springframework.stereotype.Component;

/** Between the entity and its DTO; a relation is resolved by the identifier of its target. */
@Component
public class BranchMapper {

  private final BranchRepository branches;

  public BranchMapper(BranchRepository branches) {
    this.branches = branches;
  }

  public BranchDto toDto(Branch branch) {
    return new BranchDto(
        branch.getId(),
        branch.getCode(),
        branch.getCount(),
        branch.getOpen(),
        branch.getOpenedAt(),
        branch.getSeenAt(),
        branch.getToken(),
        branch.getRatio(),
        branch.getAmount(),
        branch.getParent() == null ? null : branch.getParent().getId(),
        branch.getCreatedAt(),
        branch.getUpdatedAt());
  }

  /** Copies the DTO onto the entity, all but its identifier and its audit times. */
  public void update(BranchDto dto, Branch branch) {
    branch.setCode(dto.code());
    branch.setCount(dto.count());
    branch.setOpen(dto.open());
    branch.setOpenedAt(dto.openedAt());
    branch.setSeenAt(dto.seenAt());
    branch.setToken(dto.token());
    branch.setRatio(dto.ratio());
    branch.setAmount(dto.amount());
    branch.setParent(dto.parentId() == null ? null : branches.getReferenceById(dto.parentId()));
  }
}
