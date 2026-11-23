// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch.adapter.out.persistence;

import me.yakar.cornerservice.branch.domain.Branch;
import org.springframework.stereotype.Component;

/**
 * Between the table and the domain: every value, the identifier and the audit times included; a
 * relation is the identifier of its target in both.
 */
@Component
public class BranchPersistenceMapper {

  public Branch toDomain(BranchEntity entity) {
    Branch branch = new Branch();
    branch.setId(entity.getId());
    branch.setCode(entity.getCode());
    branch.setCount(entity.getCount());
    branch.setOpen(entity.getOpen());
    branch.setOpenedAt(entity.getOpenedAt());
    branch.setSeenAt(entity.getSeenAt());
    branch.setToken(entity.getToken());
    branch.setRatio(entity.getRatio());
    branch.setAmount(entity.getAmount());
    branch.setMemo(entity.getMemo());
    branch.setParentId(entity.getParentId());
    branch.setCreatedAt(entity.getCreatedAt());
    branch.setUpdatedAt(entity.getUpdatedAt());
    return branch;
  }

  public BranchEntity toEntity(Branch branch) {
    BranchEntity entity = new BranchEntity();
    entity.setId(branch.getId());
    entity.setCode(branch.getCode());
    entity.setCount(branch.getCount());
    entity.setOpen(branch.getOpen());
    entity.setOpenedAt(branch.getOpenedAt());
    entity.setSeenAt(branch.getSeenAt());
    entity.setToken(branch.getToken());
    entity.setRatio(branch.getRatio());
    entity.setAmount(branch.getAmount());
    entity.setMemo(branch.getMemo());
    entity.setParentId(branch.getParentId());
    entity.setCreatedAt(branch.getCreatedAt());
    entity.setUpdatedAt(branch.getUpdatedAt());
    return entity;
  }
}
