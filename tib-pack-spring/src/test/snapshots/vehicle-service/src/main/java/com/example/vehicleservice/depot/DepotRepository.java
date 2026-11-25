// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepotRepository extends JpaRepository<Depot, UUID> {}
