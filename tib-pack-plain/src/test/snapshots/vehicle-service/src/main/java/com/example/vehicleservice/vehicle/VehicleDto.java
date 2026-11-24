// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A vehicle as the API shows and takes it; a relation is the identifier of its target. The
 * identifier and the audit times are read only; the service ignores them.
 */
public record VehicleDto(
    Long id,
    String plateNumber,
    Integer capacity,
    BigDecimal price,
    UUID depotId,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * The rules of the spec that the values break, each as the value and the rule; none when they
   * keep them all.
   */
  public List<String> problems() {
    List<String> problems = new ArrayList<>();
    if (plateNumber == null) {
      problems.add("plateNumber: required");
    } else if (plateNumber.length() > 16) {
      problems.add("plateNumber: longer than 16 characters");
    }
    return problems;
  }
}
