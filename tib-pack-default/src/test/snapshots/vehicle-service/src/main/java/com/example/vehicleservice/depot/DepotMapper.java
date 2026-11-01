// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import org.springframework.stereotype.Component;

/** Between the entity and its DTO. */
@Component
public class DepotMapper {

  public DepotDto toDto(Depot depot) {
    return new DepotDto(
        depot.getId(),
        depot.getCode(),
        depot.getOpenedOn());
  }

  /** Copies the DTO onto the entity, all but its identifier. */
  public void update(DepotDto dto, Depot depot) {
    depot.setCode(dto.code());
    depot.setOpenedOn(dto.openedOn());
  }
}
