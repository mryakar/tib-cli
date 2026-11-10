// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item;

import me.yakar.cornerservice.branch.BranchRepository;
import org.springframework.stereotype.Component;

/** Between the entity and its DTO; a relation is resolved by the identifier of its target. */
@Component
public class ItemMapper {

  private final BranchRepository branches;

  public ItemMapper(BranchRepository branches) {
    this.branches = branches;
  }

  public ItemDto toDto(Item item) {
    return new ItemDto(
        item.getId(), item.getLabel(), item.getBranch() == null ? null : item.getBranch().getId());
  }

  /** Copies the DTO onto the entity, all but its identifier. */
  public void update(ItemDto dto, Item item) {
    item.setLabel(dto.label());
    item.setBranch(dto.branchId() == null ? null : branches.getReferenceById(dto.branchId()));
  }
}
