// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item;

import me.yakar.cornerservice.branch.Branch;

/**
 * Sample items for the tests, with the first or the second sample values: a test changes the value
 * it is about through the builder.
 */
public final class ItemSamples {

  private ItemSamples() {}

  /** An item with the first sample values, and the saved entities it needs. */
  public static ItemBuilder item(Branch branch) {
    return new ItemBuilder().label("One").branch(branch);
  }

  /** An item with the second sample values, and the saved entities it needs. */
  public static ItemBuilder otherItem(Branch branch) {
    return new ItemBuilder().label("Two").branch(branch);
  }

  /**
   * An item as the API takes it, with the first sample values, and the identifiers of the entities
   * it needs.
   */
  public static ItemDtoBuilder dto(Long branchId) {
    return new ItemDtoBuilder().label("One").branchId(branchId);
  }

  /**
   * An item as the API takes it, with the second sample values, and the identifiers of the entities
   * it needs.
   */
  public static ItemDtoBuilder otherDto(Long branchId) {
    return new ItemDtoBuilder().label("Two").branchId(branchId);
  }

  /** An item as the API takes it, without any value. */
  public static ItemDtoBuilder emptyDto() {
    return new ItemDtoBuilder();
  }

  /** An item to build, one value at a time. */
  public static final class ItemBuilder {
    private String label;
    private Branch branch;

    private ItemBuilder() {}

    public ItemBuilder label(String label) {
      this.label = label;
      return this;
    }

    public ItemBuilder branch(Branch branch) {
      this.branch = branch;
      return this;
    }

    public Item build() {
      Item item = new Item();
      item.setLabel(label);
      item.setBranch(branch);
      return item;
    }
  }

  /** An item as the API takes it, to build one value at a time. */
  public static final class ItemDtoBuilder {
    private Long id;
    private String label;
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

    public ItemDtoBuilder branchId(Long branchId) {
      this.branchId = branchId;
      return this;
    }

    public ItemDto build() {
      return new ItemDto(id, label, branchId);
    }
  }
}
