// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.depot.DepotRepository;
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
  private final DepotRepository depots;

  public VehicleService(VehicleRepository vehicles, VehicleMapper mapper, DepotRepository depots) {
    this.vehicles = vehicles;
    this.mapper = mapper;
    this.depots = depots;
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
    apply(dto, vehicle);
    return mapper.toDto(vehicles.save(vehicle));
  }

  public VehicleDto update(Long id, VehicleDto dto) {
    Vehicle vehicle = find(id);
    apply(dto, vehicle);
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

  private void apply(VehicleDto dto, Vehicle vehicle) {
    mapper.update(dto, vehicle);
    vehicle.setDepot(dto.depotId() == null ? null : depots.getReferenceById(dto.depotId()));
  }
}
