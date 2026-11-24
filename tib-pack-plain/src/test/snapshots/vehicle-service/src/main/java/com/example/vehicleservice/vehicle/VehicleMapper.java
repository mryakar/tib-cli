// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

/** Between the entity and its DTO; a relation is the identifier of its target in both. */
public class VehicleMapper {

  public VehicleDto toDto(Vehicle vehicle) {
    return new VehicleDto(
        vehicle.getId(),
        vehicle.getPlateNumber(),
        vehicle.getCapacity(),
        vehicle.getPrice(),
        vehicle.getDepotId(),
        vehicle.getCreatedAt(),
        vehicle.getUpdatedAt());
  }

  /** Copies the DTO onto the entity, all but its identifier and its audit times. */
  public void update(VehicleDto dto, Vehicle vehicle) {
    vehicle.setPlateNumber(dto.plateNumber());
    vehicle.setCapacity(dto.capacity());
    vehicle.setPrice(dto.price());
    vehicle.setDepotId(dto.depotId());
  }
}
