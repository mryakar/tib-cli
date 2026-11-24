// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item.web;

import me.yakar.cornerservice.item.domain.Item;

/** Between the entity and its DTO; a relation is the identifier of its target in both. */
public class ItemMapper {

  public ItemDto toDto(Item item) {
    return new ItemDto(item.getId(), item.getLabel(), item.getItem(), item.getBranchId());
  }

  /** Copies the DTO onto the entity, all but its identifier. */
  public void update(ItemDto dto, Item item) {
    item.setLabel(dto.label());
    item.setItem(dto.item());
    item.setBranchId(dto.branchId());
  }
}
