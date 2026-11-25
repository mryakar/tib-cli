// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag;

import me.yakar.cornerservice.branch.BranchRepository;
import org.springframework.stereotype.Component;

/** Between the entity and its DTO; a relation is resolved by the identifier of its target. */
@Component
public class TagMapper {

  private final BranchRepository branches;

  public TagMapper(BranchRepository branches) {
    this.branches = branches;
  }

  public TagDto toDto(Tag tag) {
    return new TagDto(
        tag.getId(),
        tag.getLabel(),
        tag.getBranch() == null ? null : tag.getBranch().getId(),
        tag.getOther() == null ? null : tag.getOther().getId());
  }

  /** Copies the DTO onto the entity, all but its identifier. */
  public void update(TagDto dto, Tag tag) {
    tag.setLabel(dto.label());
    tag.setBranch(dto.branchId() == null ? null : branches.getReferenceById(dto.branchId()));
    tag.setOther(dto.otherId() == null ? null : branches.getReferenceById(dto.otherId()));
  }
}
