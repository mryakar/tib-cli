// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.depot.DepotRepository;
import org.springframework.stereotype.Component;

/** Between the entity and its DTO; a relation is resolved by the identifier of its target. */
@Component
public class VehicleMapper {

  private final DepotRepository depots;

  public VehicleMapper(DepotRepository depots) {
    this.depots = depots;
  }

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

  /** Copies the DTO onto the entity, all but its identifier and its audit times. */
  public void update(VehicleDto dto, Vehicle vehicle) {
    vehicle.setPlateNumber(dto.plateNumber());
    vehicle.setCapacity(dto.capacity());
    vehicle.setPrice(dto.price());
    vehicle.setDepot(dto.depotId() == null ? null : depots.getReferenceById(dto.depotId()));
  }
}
