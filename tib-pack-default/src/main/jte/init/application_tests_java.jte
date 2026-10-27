@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.DefaultPack
@param Project project
@param DefaultPack pack
// Written by tib when the service was created; it is yours from then on.
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
