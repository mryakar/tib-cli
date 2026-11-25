@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ${project.applicationClass()}Tests {

  @Test
  void contextLoads() {}
}
