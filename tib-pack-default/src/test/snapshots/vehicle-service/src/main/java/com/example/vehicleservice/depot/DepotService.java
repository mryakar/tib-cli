// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of depots. */
@Service
@Transactional
public class DepotService {

  private final DepotRepository depots;
  private final DepotMapper mapper;

  public DepotService(DepotRepository depots, DepotMapper mapper) {
    this.depots = depots;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<DepotDto> list(Pageable pageable) {
    return depots.findAll(pageable).map(mapper::toDto);
  }

  @Transactional(readOnly = true)
  public DepotDto get(UUID id) {
    return mapper.toDto(find(id));
  }

  public DepotDto create(DepotDto dto) {
    Depot depot = new Depot();
    apply(dto, depot);
    return mapper.toDto(depots.save(depot));
  }

  public DepotDto update(UUID id, DepotDto dto) {
    Depot depot = find(id);
    apply(dto, depot);
    return mapper.toDto(depot);
  }

  public void delete(UUID id) {
    depots.delete(find(id));
  }

  private Depot find(UUID id) {
    return depots
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no depot " + id));
  }

  private void apply(DepotDto dto, Depot depot) {
    mapper.update(dto, depot);
  }
}
