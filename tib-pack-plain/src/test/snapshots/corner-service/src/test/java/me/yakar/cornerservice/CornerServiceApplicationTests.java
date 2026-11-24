// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The service starts against PostgreSQL in a container, and answers. */
class CornerServiceApplicationTests {

  @Test
  void startsAndAnswers() throws Exception {
    try (TestService service = TestService.start()) {
      // Nothing is at the root: the API of the entities is under /api.
      assertEquals(404, service.send("GET", "/").statusCode());
    }
  }
}
