// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import java.time.LocalDate;
import java.util.UUID;

/** The columns of DEPOTS, as the spec names them, in plain Java; {@link Depot} is the entity. */
public abstract class DepotGenerated {

  private UUID id;
  private String code;
  private LocalDate openedOn;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public LocalDate getOpenedOn() {
    return openedOn;
  }

  public void setOpenedOn(LocalDate openedOn) {
    this.openedOn = openedOn;
  }
}
