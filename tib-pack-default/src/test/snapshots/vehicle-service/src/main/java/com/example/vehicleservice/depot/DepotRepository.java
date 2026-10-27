// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepotRepository extends JpaRepository<Depot, UUID> {}
