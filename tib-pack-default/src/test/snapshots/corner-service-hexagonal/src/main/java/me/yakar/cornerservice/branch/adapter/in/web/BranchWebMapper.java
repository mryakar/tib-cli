// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch.adapter.in.web;

import me.yakar.cornerservice.branch.domain.Branch;
import org.springframework.stereotype.Component;

/** Between the entity and its DTO; a relation is the identifier of its target in both. */
@Component
public class BranchWebMapper {

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
        branch.getMemo(),
        branch.getParentId(),
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
    branch.setMemo(dto.memo());
    branch.setParentId(dto.parentId());
  }
}
