// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item.adapter.out.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.item.domain.Item;

/**
 * The table ITEM, in SQL, on the connection of the unit of work that {@link Transactions} runs; a
 * relation is the identifier of its target.
 */
public class ItemRepository {

  private static final String COLUMNS = "ID, LABEL, ITEM, BRANCH_ID";

  private final Transactions transactions;

  public ItemRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** The page of items with the number, from 0, in the order of their identifiers. */
  public Page<Item> findAll(int number, int size) throws SQLException {
    List<Item> items = new ArrayList<>();
    try (PreparedStatement statement =
        connection()
            .prepareStatement("SELECT " + COLUMNS + " FROM ITEM ORDER BY ID LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          items.add(read(rows));
        }
      }
    }
    return Page.of(items, number, size, count());
  }

  /** How many items there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("SELECT COUNT(*) FROM ITEM");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Item> findById(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM ITEM WHERE ID = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the item, which gets what the database gives its row, and gives it. */
  public Item insert(Item item) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "INSERT INTO ITEM (LABEL, ITEM, BRANCH_ID) VALUES (?, ?, ?) RETURNING ID")) {
      statement.setObject(1, item.getLabel());
      statement.setObject(2, item.getItem());
      statement.setObject(3, item.getBranchId());
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        item.setId(rows.getObject("ID", Long.class));
      }
    }
    return item;
  }

  /**
   * Updates the row of the item, all but its identifier.
   *
   * @return whether the table has the row
   */
  public boolean update(Item item) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement("UPDATE ITEM SET LABEL = ?, ITEM = ?, BRANCH_ID = ? WHERE ID = ?")) {
      statement.setObject(1, item.getLabel());
      statement.setObject(2, item.getItem());
      statement.setObject(3, item.getBranchId());
      statement.setObject(4, item.getId());
      return statement.executeUpdate() == 1;
    }
  }

  /**
   * Deletes the row of the item with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM ITEM WHERE ID = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  /** Deletes every item. */
  public void deleteAll() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("DELETE FROM ITEM")) {
      statement.executeUpdate();
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The item of the row, which has the columns of {@link #COLUMNS}. */
  private static Item read(ResultSet rows) throws SQLException {
    Item item = new Item();
    item.setId(rows.getObject("ID", Long.class));
    item.setLabel(rows.getObject("LABEL", String.class));
    item.setItem(rows.getObject("ITEM", String.class));
    item.setBranchId(rows.getObject("BRANCH_ID", Long.class));
    return item;
  }
}
