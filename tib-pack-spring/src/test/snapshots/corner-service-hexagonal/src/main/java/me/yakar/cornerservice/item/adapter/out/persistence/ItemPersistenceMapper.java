// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item.adapter.out.persistence;

import me.yakar.cornerservice.item.domain.Item;
import org.springframework.stereotype.Component;

/**
 * Between the table and the domain: every value, the identifier included; a relation is the
 * identifier of its target in both.
 */
@Component
public class ItemPersistenceMapper {

  public Item toDomain(ItemEntity entity) {
    Item item = new Item();
    item.setId(entity.getId());
    item.setLabel(entity.getLabel());
    item.setItem(entity.getItem());
    item.setBranchId(entity.getBranchId());
    return item;
  }

  public ItemEntity toEntity(Item item) {
    ItemEntity entity = new ItemEntity();
    entity.setId(item.getId());
    entity.setLabel(item.getLabel());
    entity.setItem(item.getItem());
    entity.setBranchId(item.getBranchId());
    return entity;
  }
}
