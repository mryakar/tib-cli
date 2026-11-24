// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import java.io.IOException;

/**
 * Runs the service against PostgreSQL in a container, as the tests do, on the port of {@code PORT}:
 * {@code ./gradlew testRun}.
 */
public class TestCornerServiceApplication {

  public static void main(String[] args) throws IOException {
    CornerServiceApplication.start(TestDatabase.config(Config.fromEnvironment().port()));
  }
}
