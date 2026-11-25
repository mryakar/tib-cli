// Written by tib when the service was created; it is yours from then on.
package com.example.vehicleservice;

import org.springframework.boot.SpringApplication;

/** Runs the service against PostgreSQL in a container, as the tests do: {@code bootTestRun}. */
public class TestVehicleServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(VehicleServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
