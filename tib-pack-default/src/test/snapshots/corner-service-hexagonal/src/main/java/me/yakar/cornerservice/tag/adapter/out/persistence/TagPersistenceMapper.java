// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag.adapter.out.persistence;

import me.yakar.cornerservice.tag.domain.Tag;
import org.springframework.stereotype.Component;

/**
 * Between the table and the domain: every value, the identifier included; a relation is the
 * identifier of its target in both.
 */
@Component
public class TagPersistenceMapper {

  public Tag toDomain(TagEntity entity) {
    Tag tag = new Tag();
    tag.setId(entity.getId());
    tag.setLabel(entity.getLabel());
    tag.setBranchId(entity.getBranchId());
    tag.setOtherId(entity.getOtherId());
    return tag;
  }

  public TagEntity toEntity(Tag tag) {
    TagEntity entity = new TagEntity();
    entity.setId(tag.getId());
    entity.setLabel(tag.getLabel());
    entity.setBranchId(tag.getBranchId());
    entity.setOtherId(tag.getOtherId());
    return entity;
  }
}
