// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag.adapter.out.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.tag.domain.Tag;

/**
 * The table TAG, in SQL, on the connection of the unit of work that {@link Transactions} runs; a
 * relation is the identifier of its target.
 */
public class TagRepository {

  private static final String COLUMNS = "ID, LABEL, BRANCH_ID, OTHER_ID";

  private final Transactions transactions;

  public TagRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** The page of tags with the number, from 0, in the order of their identifiers. */
  public Page<Tag> findAll(int number, int size) throws SQLException {
    List<Tag> tags = new ArrayList<>();
    try (PreparedStatement statement =
        connection()
            .prepareStatement("SELECT " + COLUMNS + " FROM TAG ORDER BY ID LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          tags.add(read(rows));
        }
      }
    }
    return Page.of(tags, number, size, count());
  }

  /** How many tags there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("SELECT COUNT(*) FROM TAG");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Tag> findById(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM TAG WHERE ID = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the tag, which gets what the database gives its row, and gives it. */
  public Tag insert(Tag tag) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "INSERT INTO TAG (LABEL, BRANCH_ID, OTHER_ID) VALUES (?, ?, ?) RETURNING ID")) {
      statement.setObject(1, tag.getLabel());
      statement.setObject(2, tag.getBranchId());
      statement.setObject(3, tag.getOtherId());
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        tag.setId(rows.getObject("ID", Long.class));
      }
    }
    return tag;
  }

  /**
   * Updates the row of the tag, all but its identifier.
   *
   * @return whether the table has the row
   */
  public boolean update(Tag tag) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "UPDATE TAG SET LABEL = ?, BRANCH_ID = ?, OTHER_ID = ? WHERE ID = ?")) {
      statement.setObject(1, tag.getLabel());
      statement.setObject(2, tag.getBranchId());
      statement.setObject(3, tag.getOtherId());
      statement.setObject(4, tag.getId());
      return statement.executeUpdate() == 1;
    }
  }

  /**
   * Deletes the row of the tag with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM TAG WHERE ID = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The tag of the row, which has the columns of {@link #COLUMNS}. */
  private static Tag read(ResultSet rows) throws SQLException {
    Tag tag = new Tag();
    tag.setId(rows.getObject("ID", Long.class));
    tag.setLabel(rows.getObject("LABEL", String.class));
    tag.setBranchId(rows.getObject("BRANCH_ID", Long.class));
    tag.setOtherId(rows.getObject("OTHER_ID", Long.class));
    return tag;
  }
}
