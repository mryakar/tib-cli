// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A vehicle. The columns are in {@link VehicleGenerated}; the behavior goes here. */
@Entity
@Table(name = "VEHICLE")
public class Vehicle extends VehicleGenerated {}
