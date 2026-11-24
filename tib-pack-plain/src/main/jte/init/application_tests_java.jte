@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The service starts against PostgreSQL in a container, and answers. */
class ${project.applicationClass()}Tests {

  @Test
  void startsAndAnswers() throws Exception {
    try (TestService service = TestService.start()) {
      // Nothing is at the root: the API of the entities is under /api.
      assertEquals(404, service.send("GET", "/").statusCode());
    }
  }
}
