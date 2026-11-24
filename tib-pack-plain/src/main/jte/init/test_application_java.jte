@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import java.io.IOException;

/**
 * Runs the service against PostgreSQL in a container, as the tests do, on the port of {@code
 * PORT}: {@code ./gradlew testRun}.
 */
public class Test${project.applicationClass()} {

  public static void main(String[] args) throws IOException {
    ${project.applicationClass()}.start(TestDatabase.config(Config.fromEnvironment().port()));
  }
}
