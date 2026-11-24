@import me.yakar.tib.generate.Project
@param Project project
!{String database = project.name().replace('-', '_');}
package ${project.basePackage()};

import java.util.Map;

/**
 * The configuration of the service, from environment variables, each with a default for a machine
 * of development.
 *
 * @param databaseUrl the JDBC URL of the database: {@code DATABASE_URL}
 * @param databaseUser the user of the database: {@code DATABASE_USER}
 * @param databasePassword the password of the user: {@code DATABASE_PASSWORD}
 * @param port the port of the API: {@code PORT}; on 0, a free one
 */
public record Config(String databaseUrl, String databaseUser, String databasePassword, int port) {

  /** The configuration from the environment of the process. */
  public static Config fromEnvironment() {
    return of(System.getenv());
  }

  /** The configuration from the variables, with the default of each that is not there. */
  public static Config of(Map<String, String> environment) {
    return new Config(
        environment.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/${database}"),
        environment.getOrDefault("DATABASE_USER", "${database}"),
        environment.getOrDefault("DATABASE_PASSWORD", "${database}"),
        Integer.parseInt(environment.getOrDefault("PORT", "8080")));
  }
}
