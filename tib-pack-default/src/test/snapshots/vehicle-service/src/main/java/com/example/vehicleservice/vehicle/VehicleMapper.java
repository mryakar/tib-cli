// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

import org.springframework.stereotype.Component;

/** Between the entity and its DTO. The relations are the service's to resolve. */
@Component
public class VehicleMapper {

  public VehicleDto toDto(Vehicle vehicle) {
    return new VehicleDto(
        vehicle.getId(),
        vehicle.getPlateNumber(),
        vehicle.getCapacity(),
        vehicle.getPrice(),
        vehicle.getDepot() == null ? null : vehicle.getDepot().getId(),
        vehicle.getCreatedAt(),
        vehicle.getUpdatedAt());
  }

  /** Copies the fields of the DTO onto the entity; not its identifier, nor its relations. */
  public void update(VehicleDto dto, Vehicle vehicle) {
    vehicle.setPlateNumber(dto.plateNumber());
    vehicle.setCapacity(dto.capacity());
    vehicle.setPrice(dto.price());
  }
}
