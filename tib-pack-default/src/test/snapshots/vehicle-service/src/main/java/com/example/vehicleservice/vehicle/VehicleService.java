// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of vehicles. */
@Service
@Transactional
public class VehicleService {

  private final VehicleRepository vehicles;
  private final VehicleMapper mapper;

  public VehicleService(VehicleRepository vehicles, VehicleMapper mapper) {
    this.vehicles = vehicles;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<VehicleDto> list(Pageable pageable) {
    return vehicles.findAll(pageable).map(mapper::toDto);
  }

  @Transactional(readOnly = true)
  public VehicleDto get(Long id) {
    return mapper.toDto(find(id));
  }

  public VehicleDto create(VehicleDto dto) {
    Vehicle vehicle = new Vehicle();
    mapper.update(dto, vehicle);
    return mapper.toDto(vehicles.save(vehicle));
  }

  public VehicleDto update(Long id, VehicleDto dto) {
    Vehicle vehicle = find(id);
    mapper.update(dto, vehicle);
    return mapper.toDto(vehicle);
  }

  public void delete(Long id) {
    vehicles.delete(find(id));
  }

  private Vehicle find(Long id) {
    return vehicles
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no vehicle " + id));
  }
}
