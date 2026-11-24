// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The columns of VEHICLE, as the spec names them, in plain Java; {@link Vehicle} is the entity, and
 * a relation is the identifier of its target.
 */
public abstract class VehicleGenerated {

  private Long id;
  private String plateNumber;
  private Integer capacity;
  private BigDecimal price;
  private UUID depotId;
  private Instant createdAt;
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPlateNumber() {
    return plateNumber;
  }

  public void setPlateNumber(String plateNumber) {
    this.plateNumber = plateNumber;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public UUID getDepotId() {
    return depotId;
  }

  public void setDepotId(UUID depotId) {
    this.depotId = depotId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
