// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A depot. The columns are in {@link DepotGenerated}; the behavior goes here. */
@Entity
@Table(name = "DEPOTS")
public class Depot extends DepotGenerated {}
