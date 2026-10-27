// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch;

import org.springframework.stereotype.Component;

/** Between the entity and its DTO. The relations are the service's to resolve. */
@Component
public class BranchMapper {

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
        branch.getParent() == null ? null : branch.getParent().getId());
  }

  /** Copies the fields of the DTO onto the entity; not its identifier, nor its relations. */
  public void update(BranchDto dto, Branch branch) {
    branch.setCode(dto.code());
    branch.setCount(dto.count());
    branch.setOpen(dto.open());
    branch.setOpenedAt(dto.openedAt());
    branch.setSeenAt(dto.seenAt());
    branch.setToken(dto.token());
    branch.setRatio(dto.ratio());
    branch.setAmount(dto.amount());
  }
}
