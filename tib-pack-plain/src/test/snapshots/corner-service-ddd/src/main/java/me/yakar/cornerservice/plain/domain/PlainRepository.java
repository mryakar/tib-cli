// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.plain.domain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;

/**
 * The table PLAIN, in SQL, on the connection of the unit of work that {@link Transactions} runs.
 */
public class PlainRepository {

  private static final String COLUMNS = "ID, LABEL";

  private final Transactions transactions;

  public PlainRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** The page of plains with the number, from 0, in the order of their identifiers. */
  public Page<Plain> findAll(int number, int size) throws SQLException {
    List<Plain> plains = new ArrayList<>();
    try (PreparedStatement statement =
        connection()
            .prepareStatement("SELECT " + COLUMNS + " FROM PLAIN ORDER BY ID LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          plains.add(read(rows));
        }
      }
    }
    return Page.of(plains, number, size, count());
  }

  /** How many plains there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("SELECT COUNT(*) FROM PLAIN");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Plain> findById(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM PLAIN WHERE ID = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the plain, which gets what the database gives its row, and gives it. */
  public Plain insert(Plain plain) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("INSERT INTO PLAIN (LABEL) VALUES (?) RETURNING ID")) {
      statement.setObject(1, plain.getLabel());
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        plain.setId(rows.getObject("ID", Long.class));
      }
    }
    return plain;
  }

  /**
   * Updates the row of the plain, all but its identifier.
   *
   * @return whether the table has the row
   */
  public boolean update(Plain plain) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("UPDATE PLAIN SET LABEL = ? WHERE ID = ?")) {
      statement.setObject(1, plain.getLabel());
      statement.setObject(2, plain.getId());
      return statement.executeUpdate() == 1;
    }
  }

  /**
   * Deletes the row of the plain with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM PLAIN WHERE ID = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The plain of the row, which has the columns of {@link #COLUMNS}. */
  private static Plain read(ResultSet rows) throws SQLException {
    Plain plain = new Plain();
    plain.setId(rows.getObject("ID", Long.class));
    plain.setLabel(rows.getObject("LABEL", String.class));
    return plain;
  }
}
