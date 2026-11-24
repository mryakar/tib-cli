// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.Page;
import com.example.vehicleservice.Transactions;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The table VEHICLE, in SQL, on the connection of the unit of work that {@link Transactions} runs;
 * a relation is the identifier of its target. The SQL sets the audit times.
 */
public class VehicleRepository {

  private static final String COLUMNS =
      "ID, PLATE_NUMBER, CAPACITY, PRICE, DEPOT_ID, CREATED_AT, UPDATED_AT";

  private final Transactions transactions;

  public VehicleRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** The page of vehicles with the number, from 0, in the order of their identifiers. */
  public Page<Vehicle> findAll(int number, int size) throws SQLException {
    List<Vehicle> vehicles = new ArrayList<>();
    try (PreparedStatement statement =
        connection()
            .prepareStatement("SELECT " + COLUMNS + " FROM VEHICLE ORDER BY ID LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          vehicles.add(read(rows));
        }
      }
    }
    return Page.of(vehicles, number, size, count());
  }

  /** How many vehicles there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement =
            connection().prepareStatement("SELECT COUNT(*) FROM VEHICLE");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Vehicle> findById(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM VEHICLE WHERE ID = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the vehicle, which gets what the database gives its row, and gives it. */
  public Vehicle insert(Vehicle vehicle) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "INSERT INTO VEHICLE (PLATE_NUMBER, CAPACITY, PRICE, DEPOT_ID, CREATED_AT,"
                    + " UPDATED_AT) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
                    + " RETURNING ID, CREATED_AT, UPDATED_AT")) {
      statement.setObject(1, vehicle.getPlateNumber());
      statement.setObject(2, vehicle.getCapacity());
      statement.setObject(3, vehicle.getPrice());
      statement.setObject(4, vehicle.getDepotId());
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        vehicle.setId(rows.getObject("ID", Long.class));
        vehicle.setCreatedAt(rows.getObject("CREATED_AT", OffsetDateTime.class).toInstant());
        vehicle.setUpdatedAt(rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant());
      }
    }
    return vehicle;
  }

  /**
   * Updates the row of the vehicle, all but its identifier and its creation time; the vehicle gets
   * the time of the change.
   *
   * @return whether the table has the row
   */
  public boolean update(Vehicle vehicle) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "UPDATE VEHICLE SET PLATE_NUMBER = ?, CAPACITY = ?, PRICE = ?, DEPOT_ID = ?,"
                    + " UPDATED_AT = CURRENT_TIMESTAMP WHERE ID = ? RETURNING UPDATED_AT")) {
      statement.setObject(1, vehicle.getPlateNumber());
      statement.setObject(2, vehicle.getCapacity());
      statement.setObject(3, vehicle.getPrice());
      statement.setObject(4, vehicle.getDepotId());
      statement.setObject(5, vehicle.getId());
      try (ResultSet rows = statement.executeQuery()) {
        if (!rows.next()) {
          return false;
        }
        vehicle.setUpdatedAt(rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant());
        return true;
      }
    }
  }

  /**
   * Deletes the row of the vehicle with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM VEHICLE WHERE ID = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  /** Deletes every vehicle. */
  public void deleteAll() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("DELETE FROM VEHICLE")) {
      statement.executeUpdate();
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The vehicle of the row, which has the columns of {@link #COLUMNS}. */
  private static Vehicle read(ResultSet rows) throws SQLException {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(rows.getObject("ID", Long.class));
    vehicle.setPlateNumber(rows.getObject("PLATE_NUMBER", String.class));
    vehicle.setCapacity(rows.getObject("CAPACITY", Integer.class));
    vehicle.setPrice(rows.getObject("PRICE", BigDecimal.class));
    vehicle.setDepotId(rows.getObject("DEPOT_ID", UUID.class));
    vehicle.setCreatedAt(rows.getObject("CREATED_AT", OffsetDateTime.class).toInstant());
    vehicle.setUpdatedAt(rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant());
    return vehicle;
  }
}
