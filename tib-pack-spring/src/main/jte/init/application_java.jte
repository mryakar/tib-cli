@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${project.applicationClass()} {

  public static void main(String[] args) {
    SpringApplication.run(${project.applicationClass()}.class, args);
  }
}
