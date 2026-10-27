// Written by tib on every run. Do not edit: your changes would be lost. Your code goes in Depot.
package com.example.vehicleservice.depot;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.util.UUID;

/** The columns of DEPOTS, as the spec names them; {@link Depot} is the entity. */
@MappedSuperclass
public abstract class DepotGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "ID", nullable = false)
  private UUID id;

  @Column(name = "DEPOT_CODE", nullable = false, unique = true, length = 8)
  private String code;

  @Column(name = "OPENED_ON")
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
