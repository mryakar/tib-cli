// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import org.springframework.stereotype.Component;

/** Between the entity and its DTO. The relations are the service's to resolve. */
@Component
public class DepotMapper {

  public DepotDto toDto(Depot depot) {
    return new DepotDto(
        depot.getId(),
        depot.getCode(),
        depot.getOpenedOn());
  }

  /** Copies the fields of the DTO onto the entity; not its identifier, nor its relations. */
  public void update(DepotDto dto, Depot depot) {
    depot.setCode(dto.code());
    depot.setOpenedOn(dto.openedOn());
  }
}
