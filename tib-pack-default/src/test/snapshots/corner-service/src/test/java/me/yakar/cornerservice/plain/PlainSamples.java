// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.plain;

/**
 * Sample plains for the tests, with the first or the second sample values: a test changes the value
 * it is about through the builder.
 */
public final class PlainSamples {

  private PlainSamples() {}

  /** A plain with the first sample values. */
  public static PlainBuilder plain() {
    return new PlainBuilder().label("One");
  }

  /** A plain with the second sample values. */
  public static PlainBuilder otherPlain() {
    return new PlainBuilder().label("Two");
  }

  /** A plain as the API takes it, with the first sample values. */
  public static PlainDtoBuilder dto() {
    return new PlainDtoBuilder().label("One");
  }

  /** A plain as the API takes it, with the second sample values. */
  public static PlainDtoBuilder otherDto() {
    return new PlainDtoBuilder().label("Two");
  }

  /** A plain as the API takes it, without any value. */
  public static PlainDtoBuilder emptyDto() {
    return new PlainDtoBuilder();
  }

  /** A plain to build, one value at a time. */
  public static final class PlainBuilder {
    private String label;

    private PlainBuilder() {}

    public PlainBuilder label(String label) {
      this.label = label;
      return this;
    }

    public Plain build() {
      Plain plain = new Plain();
      plain.setLabel(label);
      return plain;
    }
  }

  /** A plain as the API takes it, to build one value at a time. */
  public static final class PlainDtoBuilder {
    private Long id;
    private String label;

    private PlainDtoBuilder() {}

    public PlainDtoBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public PlainDtoBuilder label(String label) {
      this.label = label;
      return this;
    }

    public PlainDto build() {
      return new PlainDto(id, label);
    }
  }
}
