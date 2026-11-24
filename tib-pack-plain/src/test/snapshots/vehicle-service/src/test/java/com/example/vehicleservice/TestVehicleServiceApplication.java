// Written by tib when the service was created; it is yours from then on.
package com.example.vehicleservice;

import java.io.IOException;

/**
 * Runs the service against PostgreSQL in a container, as the tests do, on the port of {@code PORT}:
 * {@code ./gradlew testRun}.
 */
public class TestVehicleServiceApplication {

  public static void main(String[] args) throws IOException {
    VehicleServiceApplication.start(TestDatabase.config(Config.fromEnvironment().port()));
  }
}
