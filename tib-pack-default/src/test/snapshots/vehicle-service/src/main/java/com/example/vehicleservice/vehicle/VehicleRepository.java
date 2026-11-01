// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {}
