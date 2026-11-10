// Written by tib on every run. Do not edit: your changes would be lost. A test changes a sample
// through its builder.
package com.example.vehicleservice.depot;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Sample depots for the tests, with the first or the second sample values: a test changes the value
 * it is about through the builder.
 */
public final class DepotSamples {

  private DepotSamples() {}

  /** A depot with the first sample values. */
  public static DepotBuilder depot() {
    return new DepotBuilder().code("One").openedOn(LocalDate.of(2026, 1, 1));
  }

  /** A depot with the second sample values. */
  public static DepotBuilder otherDepot() {
    return new DepotBuilder().code("Two").openedOn(LocalDate.of(2026, 1, 2));
  }

  /** A depot as the API takes it, with the first sample values. */
  public static DepotDtoBuilder dto() {
    return new DepotDtoBuilder().code("One").openedOn(LocalDate.of(2026, 1, 1));
  }

  /** A depot as the API takes it, with the second sample values. */
  public static DepotDtoBuilder otherDto() {
    return new DepotDtoBuilder().code("Two").openedOn(LocalDate.of(2026, 1, 2));
  }

  /** A depot as the API takes it, without any value. */
  public static DepotDtoBuilder emptyDto() {
    return new DepotDtoBuilder();
  }

  /** A depot to build, one value at a time. */
  public static final class DepotBuilder {
    private String code;
    private LocalDate openedOn;

    private DepotBuilder() {}

    public DepotBuilder code(String code) {
      this.code = code;
      return this;
    }

    public DepotBuilder openedOn(LocalDate openedOn) {
      this.openedOn = openedOn;
      return this;
    }

    public Depot build() {
      Depot depot = new Depot();
      depot.setCode(code);
      depot.setOpenedOn(openedOn);
      return depot;
    }
  }

  /** A depot as the API takes it, to build one value at a time. */
  public static final class DepotDtoBuilder {
    private UUID id;
    private String code;
    private LocalDate openedOn;

    private DepotDtoBuilder() {}

    public DepotDtoBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public DepotDtoBuilder code(String code) {
      this.code = code;
      return this;
    }

    public DepotDtoBuilder openedOn(LocalDate openedOn) {
      this.openedOn = openedOn;
      return this;
    }

    public DepotDto build() {
      return new DepotDto(id, code, openedOn);
    }
  }
}
