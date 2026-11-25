// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item;

import me.yakar.cornerservice.item.adapter.in.web.ItemDto;
import me.yakar.cornerservice.item.domain.Item;

/**
 * Sample items for the tests, with the first or the second sample values: a test changes the value
 * it is about through the builder.
 */
public final class ItemSamples {

  private ItemSamples() {}

  /** An item with the first sample values, and the identifiers of the saved entities it needs. */
  public static ItemBuilder item(Long branchId) {
    return new ItemBuilder().label("One").item("One").branchId(branchId);
  }

  /** An item with the second sample values, and the identifiers of the saved entities it needs. */
  public static ItemBuilder otherItem(Long branchId) {
    return new ItemBuilder().label("Two").item("Two").branchId(branchId);
  }

  /**
   * An item as the API takes it, with the first sample values, and the identifiers of the entities
   * it needs.
   */
  public static ItemDtoBuilder dto(Long branchId) {
    return new ItemDtoBuilder().label("One").item("One").branchId(branchId);
  }

  /**
   * An item as the API takes it, with the second sample values, and the identifiers of the entities
   * it needs.
   */
  public static ItemDtoBuilder otherDto(Long branchId) {
    return new ItemDtoBuilder().label("Two").item("Two").branchId(branchId);
  }

  /** An item as the API takes it, without any value. */
  public static ItemDtoBuilder emptyDto() {
    return new ItemDtoBuilder();
  }

  /** An item to build, one value at a time. */
  public static final class ItemBuilder {
    private String label;
    private String item;
    private Long branchId;

    private ItemBuilder() {}

    public ItemBuilder label(String label) {
      this.label = label;
      return this;
    }

    public ItemBuilder item(String item) {
      this.item = item;
      return this;
    }

    public ItemBuilder branchId(Long branchId) {
      this.branchId = branchId;
      return this;
    }

    public Item build() {
      Item item = new Item();
      item.setLabel(this.label);
      item.setItem(this.item);
      item.setBranchId(this.branchId);
      return item;
    }
  }

  /** An item as the API takes it, to build one value at a time. */
  public static final class ItemDtoBuilder {
    private Long id;
    private String label;
    private String item;
    private Long branchId;

    private ItemDtoBuilder() {}

    public ItemDtoBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public ItemDtoBuilder label(String label) {
      this.label = label;
      return this;
    }

    public ItemDtoBuilder item(String item) {
      this.item = item;
      return this;
    }

    public ItemDtoBuilder branchId(Long branchId) {
      this.branchId = branchId;
      return this;
    }

    public ItemDto build() {
      return new ItemDto(id, label, item, branchId);
    }
  }
}
