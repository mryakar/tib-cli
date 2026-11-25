// Written by tib when the service was created; it is yours from then on.
package com.example.vehicleservice;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Runs units of work against the database, each on one connection, in one transaction: the work
 * commits when it returns and rolls back when it throws. While a unit of work runs, the
 * repositories that it calls on its thread take its connection from {@link #connection()}; a unit
 * of work that runs inside another joins it.
 */
public class Transactions {

  private final DataSource dataSource;
  private final ThreadLocal<Connection> current = new ThreadLocal<>();

  public Transactions(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Runs the work in a transaction of its own, or in the one that runs on this thread, and gives
   * what it gives.
   *
   * @throws DatabaseException when the database fails, as on a broken constraint; the transaction
   *     is rolled back, as it is when the work throws anything else
   */
  public <T> T run(Work<T> work) {
    if (current.get() != null) {
      try {
        return work.run();
      } catch (SQLException e) {
        throw new DatabaseException(e);
      }
    }
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      current.set(connection);
      try {
        T result = work.run();
        connection.commit();
        return result;
      } catch (SQLException | RuntimeException e) {
        rollback(connection, e);
        throw e;
      } finally {
        current.remove();
      }
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  /**
   * The connection of the unit of work that runs on this thread.
   *
   * @throws IllegalStateException when none runs
   */
  public Connection connection() {
    Connection connection = current.get();
    if (connection == null) {
      throw new IllegalStateException("no unit of work runs on this thread");
    }
    return connection;
  }

  private static void rollback(Connection connection, Exception failure) {
    try {
      connection.rollback();
    } catch (SQLException e) {
      failure.addSuppressed(e);
    }
  }

  /** A unit of work on the database, which may fail as JDBC does. */
  @FunctionalInterface
  public interface Work<T> {

    T run() throws SQLException;
  }

  /** The failure of the database in a unit of work. */
  public static final class DatabaseException extends RuntimeException {

    public DatabaseException(SQLException cause) {
      super(cause.getMessage(), cause);
    }

    @Override
    public synchronized SQLException getCause() {
      return (SQLException) super.getCause();
    }
  }
}
