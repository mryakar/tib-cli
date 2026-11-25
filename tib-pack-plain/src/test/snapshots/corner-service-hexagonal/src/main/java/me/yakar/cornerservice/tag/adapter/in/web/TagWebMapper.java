// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag.adapter.in.web;

import me.yakar.cornerservice.tag.domain.Tag;

/** Between the entity and its DTO; a relation is the identifier of its target in both. */
public class TagWebMapper {

  public TagDto toDto(Tag tag) {
    return new TagDto(tag.getId(), tag.getLabel(), tag.getBranchId(), tag.getOtherId());
  }

  /** Copies the DTO onto the entity, all but its identifier. */
  public void update(TagDto dto, Tag tag) {
    tag.setLabel(dto.label());
    tag.setBranchId(dto.branchId());
    tag.setOtherId(dto.otherId());
  }
}
