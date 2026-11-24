// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A depot as the API shows and takes it. The identifier is read only; the service ignores it. */
public record DepotDto(UUID id, String code, LocalDate openedOn) {

  /**
   * The rules of the spec that the values break, each as the value and the rule; none when they
   * keep them all.
   */
  public List<String> problems() {
    List<String> problems = new ArrayList<>();
    if (code == null) {
      problems.add("code: required");
    } else if (code.length() > 8) {
      problems.add("code: longer than 8 characters");
    }
    return problems;
  }
}
