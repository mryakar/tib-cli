// Written by tib on every run. Do not edit: your changes would be lost. A test changes a sample
// through its builder.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.depot.Depot;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sample vehicles for the tests, with the first or the second sample values: a test changes the
 * value it is about through the builder.
 */
public final class VehicleSamples {

  private VehicleSamples() {}

  /** A vehicle with the first sample values. */
  public static VehicleBuilder vehicle() {
    return new VehicleBuilder().plateNumber("One").capacity(1).price(new BigDecimal("1.00"));
  }

  /** A vehicle with the second sample values. */
  public static VehicleBuilder otherVehicle() {
    return new VehicleBuilder().plateNumber("Two").capacity(2).price(new BigDecimal("2.00"));
  }

  /** A vehicle as the API takes it, with the first sample values. */
  public static VehicleDtoBuilder dto() {
    return new VehicleDtoBuilder().plateNumber("One").capacity(1).price(new BigDecimal("1.00"));
  }

  /** A vehicle as the API takes it, with the second sample values. */
  public static VehicleDtoBuilder otherDto() {
    return new VehicleDtoBuilder().plateNumber("Two").capacity(2).price(new BigDecimal("2.00"));
  }

  /** A vehicle as the API takes it, without any value. */
  public static VehicleDtoBuilder emptyDto() {
    return new VehicleDtoBuilder();
  }

  /** A vehicle to build, one value at a time. */
  public static final class VehicleBuilder {
    private String plateNumber;
    private Integer capacity;
    private BigDecimal price;
    private Depot depot;

    private VehicleBuilder() {}

    public VehicleBuilder plateNumber(String plateNumber) {
      this.plateNumber = plateNumber;
      return this;
    }

    public VehicleBuilder capacity(Integer capacity) {
      this.capacity = capacity;
      return this;
    }

    public VehicleBuilder price(BigDecimal price) {
      this.price = price;
      return this;
    }

    public VehicleBuilder depot(Depot depot) {
      this.depot = depot;
      return this;
    }

    public Vehicle build() {
      Vehicle vehicle = new Vehicle();
      vehicle.setPlateNumber(plateNumber);
      vehicle.setCapacity(capacity);
      vehicle.setPrice(price);
      vehicle.setDepot(depot);
      return vehicle;
    }
  }

  /** A vehicle as the API takes it, to build one value at a time; its audit times are none. */
  public static final class VehicleDtoBuilder {
    private Long id;
    private String plateNumber;
    private Integer capacity;
    private BigDecimal price;
    private UUID depotId;

    private VehicleDtoBuilder() {}

    public VehicleDtoBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public VehicleDtoBuilder plateNumber(String plateNumber) {
      this.plateNumber = plateNumber;
      return this;
    }

    public VehicleDtoBuilder capacity(Integer capacity) {
      this.capacity = capacity;
      return this;
    }

    public VehicleDtoBuilder price(BigDecimal price) {
      this.price = price;
      return this;
    }

    public VehicleDtoBuilder depotId(UUID depotId) {
      this.depotId = depotId;
      return this;
    }

    public VehicleDto build() {
      return new VehicleDto(id, plateNumber, capacity, price, depotId, null, null);
    }
  }
}
