@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import org.springframework.boot.SpringApplication;

/** Runs the service against PostgreSQL in a container, as the tests do: {@code bootTestRun}. */
public class Test${project.applicationClass()} {

  public static void main(String[] args) {
    SpringApplication.from(${project.applicationClass()}::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
