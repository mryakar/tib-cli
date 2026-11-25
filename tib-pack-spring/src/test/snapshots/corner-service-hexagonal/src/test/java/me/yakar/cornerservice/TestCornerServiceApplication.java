// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import org.springframework.boot.SpringApplication;

/** Runs the service against PostgreSQL in a container, as the tests do: {@code bootTestRun}. */
public class TestCornerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(CornerServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
