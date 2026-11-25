// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import javax.sql.DataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The database that the tests run against: PostgreSQL in a container, started once for all of them,
 * when the first one needs it, and migrated as the service migrates its own.
 */
public final class TestDatabase {

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

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
      dataSource = CornerServiceApplication.dataSource(config(0));
      CornerServiceApplication.migrate(dataSource);
    }
    return dataSource;
  }
}
