@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.DefaultPack
@param Project project
@param DefaultPack pack
package ${project.basePackage()};

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** The database that the tests run against: PostgreSQL in a container. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:${pack.postgresVersion()}"));
  }
}
