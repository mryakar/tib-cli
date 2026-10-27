// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag;

import org.springframework.stereotype.Component;

/** Between the entity and its DTO. The relations are the service's to resolve. */
@Component
public class TagMapper {

  public TagDto toDto(Tag tag) {
    return new TagDto(
        tag.getId(),
        tag.getLabel(),
        tag.getBranch() == null ? null : tag.getBranch().getId(),
        tag.getOther() == null ? null : tag.getOther().getId());
  }

  /** Copies the fields of the DTO onto the entity; not its identifier, nor its relations. */
  public void update(TagDto dto, Tag tag) {
    tag.setLabel(dto.label());
  }
}
