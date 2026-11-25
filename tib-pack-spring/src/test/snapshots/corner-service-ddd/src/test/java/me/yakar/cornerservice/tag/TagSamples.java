// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag;

import me.yakar.cornerservice.tag.domain.Tag;
import me.yakar.cornerservice.tag.web.TagDto;

/**
 * Sample tags for the tests, with the first or the second sample values: a test changes the value
 * it is about through the builder.
 */
public final class TagSamples {

  private TagSamples() {}

  /** A tag with the first sample values, and the identifiers of the saved entities it needs. */
  public static TagBuilder tag(Long branchId) {
    return new TagBuilder().label("One").branchId(branchId);
  }

  /** A tag with the second sample values, and the identifiers of the saved entities it needs. */
  public static TagBuilder otherTag(Long branchId) {
    return new TagBuilder().label("Two").branchId(branchId);
  }

  /**
   * A tag as the API takes it, with the first sample values, and the identifiers of the entities it
   * needs.
   */
  public static TagDtoBuilder dto(Long branchId) {
    return new TagDtoBuilder().label("One").branchId(branchId);
  }

  /**
   * A tag as the API takes it, with the second sample values, and the identifiers of the entities
   * it needs.
   */
  public static TagDtoBuilder otherDto(Long branchId) {
    return new TagDtoBuilder().label("Two").branchId(branchId);
  }

  /** A tag as the API takes it, without any value. */
  public static TagDtoBuilder emptyDto() {
    return new TagDtoBuilder();
  }

  /** A tag to build, one value at a time. */
  public static final class TagBuilder {
    private String label;
    private Long branchId;
    private Long otherId;

    private TagBuilder() {}

    public TagBuilder label(String label) {
      this.label = label;
      return this;
    }

    public TagBuilder branchId(Long branchId) {
      this.branchId = branchId;
      return this;
    }

    public TagBuilder otherId(Long otherId) {
      this.otherId = otherId;
      return this;
    }

    public Tag build() {
      Tag tag = new Tag();
      tag.setLabel(this.label);
      tag.setBranchId(this.branchId);
      tag.setOtherId(this.otherId);
      return tag;
    }
  }

  /** A tag as the API takes it, to build one value at a time. */
  public static final class TagDtoBuilder {
    private Long id;
    private String label;
    private Long branchId;
    private Long otherId;

    private TagDtoBuilder() {}

    public TagDtoBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public TagDtoBuilder label(String label) {
      this.label = label;
      return this;
    }

    public TagDtoBuilder branchId(Long branchId) {
      this.branchId = branchId;
      return this;
    }

    public TagDtoBuilder otherId(Long otherId) {
      this.otherId = otherId;
      return this;
    }

    public TagDto build() {
      return new TagDto(id, label, branchId, otherId);
    }
  }
}
