// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import jakarta.validation.Valid;
import java.util.UUID;
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

/** The API of depots. */
@RestController
@RequestMapping("/api/depots")
public class DepotController {

  private final DepotService service;

  public DepotController(DepotService service) {
    this.service = service;
  }

  @GetMapping
  public Page<DepotDto> list(Pageable pageable) {
    return service.list(pageable);
  }

  @GetMapping("/{id}")
  public DepotDto get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DepotDto create(@Valid @RequestBody DepotDto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public DepotDto update(@PathVariable UUID id, @Valid @RequestBody DepotDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
