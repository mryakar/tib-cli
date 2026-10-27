// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A vehicle as the API shows and takes it; a relation is the identifier of its target. */
public record VehicleDto(
    Long id,
    @NotNull @Size(max = 16) String plateNumber,
    Integer capacity,
    BigDecimal price,
    UUID depotId,
    Instant createdAt,
    Instant updatedAt) {}
