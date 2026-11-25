// Written by tib when the service was created; it is yours from then on.
package com.example.vehicleservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class VehicleServiceApplicationTests {

  @Test
  void contextLoads() {}
}
