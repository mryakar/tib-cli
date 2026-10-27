// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** A depot as the API shows and takes it; a relation is the identifier of its target. */
public record DepotDto(
    UUID id,
    @NotNull @Size(max = 8) String code,
    LocalDate openedOn) {}
