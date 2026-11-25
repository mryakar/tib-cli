@import me.yakar.tib.generate.Project
@import me.yakar.tib.pack.plain.PlainPack
@param Project project
@param PlainPack pack
package ${project.basePackage()};

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The database that the tests run against: PostgreSQL in a container, started once for all of
 * them, when the first one needs it, and migrated as the service migrates its own.
 */
public final class TestDatabase {

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:${pack.postgresVersion()}"));

  private static DataSource dataSource;

  private TestDatabase() {}

  /** The configuration of the service against the database, on the port; on 0, a free one. */
  public static synchronized Config config(int port) {
    if (!POSTGRES.isRunning()) {
      POSTGRES.start();
    }
    return new Config(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), port);
  }

  /** The database, migrated. */
  public static synchronized DataSource dataSource() {
    if (dataSource == null) {
      dataSource = ${project.applicationClass()}.dataSource(config(0));
      ${project.applicationClass()}.migrate(dataSource);
    }
    return dataSource;
  }

  /** Deletes every row of the tables, in the order given: a table before the ones it refers to. */
  public static void clear(String... tables) {
    try (Connection connection = dataSource().getConnection();
        Statement statement = connection.createStatement()) {
      for (String table : tables) {
        statement.executeUpdate("DELETE FROM " + table);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}
