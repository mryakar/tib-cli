// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.depot;

import com.example.vehicleservice.Page;
import com.example.vehicleservice.Transactions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The table DEPOTS, in SQL, on the connection of the unit of work that {@link Transactions} runs.
 */
public class DepotRepository {

  private static final String COLUMNS = "ID, DEPOT_CODE, OPENED_ON";

  private final Transactions transactions;

  public DepotRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** The page of depots with the number, from 0, in the order of their identifiers. */
  public Page<Depot> findAll(int number, int size) throws SQLException {
    List<Depot> depots = new ArrayList<>();
    try (PreparedStatement statement =
        connection()
            .prepareStatement("SELECT " + COLUMNS + " FROM DEPOTS ORDER BY ID LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          depots.add(read(rows));
        }
      }
    }
    return Page.of(depots, number, size, count());
  }

  /** How many depots there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement =
            connection().prepareStatement("SELECT COUNT(*) FROM DEPOTS");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Depot> findById(UUID id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM DEPOTS WHERE ID = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the depot, and gives it. */
  public Depot insert(Depot depot) throws SQLException {
    depot.setId(UUID.randomUUID());
    try (PreparedStatement statement =
        connection()
            .prepareStatement("INSERT INTO DEPOTS (ID, DEPOT_CODE, OPENED_ON) VALUES (?, ?, ?)")) {
      statement.setObject(1, depot.getId());
      statement.setObject(2, depot.getCode());
      statement.setObject(3, depot.getOpenedOn());
      statement.executeUpdate();
    }
    return depot;
  }

  /**
   * Updates the row of the depot, all but its identifier.
   *
   * @return whether the table has the row
   */
  public boolean update(Depot depot) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement("UPDATE DEPOTS SET DEPOT_CODE = ?, OPENED_ON = ? WHERE ID = ?")) {
      statement.setObject(1, depot.getCode());
      statement.setObject(2, depot.getOpenedOn());
      statement.setObject(3, depot.getId());
      return statement.executeUpdate() == 1;
    }
  }

  /**
   * Deletes the row of the depot with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(UUID id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM DEPOTS WHERE ID = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The depot of the row, which has the columns of {@link #COLUMNS}. */
  private static Depot read(ResultSet rows) throws SQLException {
    Depot depot = new Depot();
    depot.setId(rows.getObject("ID", UUID.class));
    depot.setCode(rows.getObject("DEPOT_CODE", String.class));
    depot.setOpenedOn(rows.getObject("OPENED_ON", LocalDate.class));
    return depot;
  }
}
