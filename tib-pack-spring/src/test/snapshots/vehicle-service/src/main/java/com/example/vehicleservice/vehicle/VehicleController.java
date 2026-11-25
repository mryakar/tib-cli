// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The API of vehicles. */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

  private final VehicleService service;

  public VehicleController(VehicleService service) {
    this.service = service;
  }

  @GetMapping
  public Page<VehicleDto> list(Pageable pageable) {
    return service.list(pageable);
  }

  @GetMapping("/{id}")
  public VehicleDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VehicleDto create(@Valid @RequestBody VehicleDto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public VehicleDto update(@PathVariable Long id, @Valid @RequestBody VehicleDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
