// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * The service, wired by hand, without a framework: its configuration from the environment, its
 * database migrated by Flyway, and the API of its entities, which {@link Routes} adds to the HTTP
 * server of the JDK.
 */
public class CornerServiceApplication {

  public static void main(String[] args) throws IOException {
    start(Config.fromEnvironment());
  }

  /**
   * Starts the service on the configuration, once its database is migrated, and gives its server,
   * whose {@code stop} ends it. On port 0, the server takes a free port, which its address tells.
   */
  public static HttpServer start(Config config) throws IOException {
    DataSource dataSource = dataSource(config);
    migrate(dataSource);
    HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
    Routes.add(server, new Transactions(dataSource));
    // Every request on a virtual thread of its own.
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.start();
    return server;
  }

  /** The database of the configuration, which opens a connection for each unit of work. */
  public static DataSource dataSource(Config config) {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(config.databaseUrl());
    dataSource.setUser(config.databaseUser());
    dataSource.setPassword(config.databasePassword());
    return dataSource;
  }

  /** Brings the schema of the database up to date with the migrations under db/migration. */
  public static void migrate(DataSource dataSource) {
    Flyway.configure().dataSource(dataSource).load().migrate();
  }
}
