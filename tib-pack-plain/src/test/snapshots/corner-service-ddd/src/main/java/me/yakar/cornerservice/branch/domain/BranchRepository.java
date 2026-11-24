// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch.domain;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.yakar.cornerservice.Transactions;

/**
 * The table BRANCH, in SQL, on the connection of the unit of work that {@link Transactions} runs; a
 * relation is the identifier of its target. The SQL sets the audit times.
 */
public class BranchRepository {

  private static final String COLUMNS =
      "BRANCH_KEY, CODE, COUNT, OPEN, OPENED_AT, SEEN_AT, TOKEN, RATIO, AMOUNT, MEMO, PARENT_ID,"
          + " CREATED_AT, UPDATED_AT";

  private final Transactions transactions;

  public BranchRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** Every branch, in the order of their identifiers. */
  public List<Branch> findAll() throws SQLException {
    List<Branch> branches = new ArrayList<>();
    try (PreparedStatement statement =
            connection()
                .prepareStatement("SELECT " + COLUMNS + " FROM BRANCH ORDER BY BRANCH_KEY");
        ResultSet rows = statement.executeQuery()) {
      while (rows.next()) {
        branches.add(read(rows));
      }
    }
    return branches;
  }

  /** How many branches there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement =
            connection().prepareStatement("SELECT COUNT(*) FROM BRANCH");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Branch> findById(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM BRANCH WHERE BRANCH_KEY = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the branch, which gets what the database gives its row, and gives it. */
  public Branch insert(Branch branch) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "INSERT INTO BRANCH (BRANCH_KEY, CODE, COUNT, OPEN, OPENED_AT, SEEN_AT, TOKEN,"
                    + " RATIO, AMOUNT, MEMO, PARENT_ID, CREATED_AT, UPDATED_AT) VALUES"
                    + " (nextval('BRANCH_SEQ'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,"
                    + " CURRENT_TIMESTAMP) RETURNING BRANCH_KEY, CREATED_AT, UPDATED_AT")) {
      statement.setObject(1, branch.getCode());
      statement.setObject(2, branch.getCount());
      statement.setObject(3, branch.getOpen());
      statement.setObject(4, branch.getOpenedAt());
      statement.setObject(5, offset(branch.getSeenAt()));
      statement.setObject(6, branch.getToken());
      statement.setObject(7, branch.getRatio());
      statement.setObject(8, branch.getAmount());
      statement.setObject(9, branch.getMemo());
      statement.setObject(10, branch.getParentId());
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        branch.setId(rows.getObject("BRANCH_KEY", Long.class));
        branch.setCreatedAt(rows.getObject("CREATED_AT", OffsetDateTime.class).toInstant());
        branch.setUpdatedAt(rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant());
      }
    }
    return branch;
  }

  /**
   * Updates the row of the branch, all but its identifier and its creation time; the branch gets
   * the time of the change.
   *
   * @return whether the table has the row
   */
  public boolean update(Branch branch) throws SQLException {
    try (PreparedStatement statement =
        connection()
            .prepareStatement(
                "UPDATE BRANCH SET CODE = ?, COUNT = ?, OPEN = ?, OPENED_AT = ?, SEEN_AT = ?, TOKEN"
                    + " = ?, RATIO = ?, AMOUNT = ?, MEMO = ?, PARENT_ID = ?, UPDATED_AT ="
                    + " CURRENT_TIMESTAMP WHERE BRANCH_KEY = ? RETURNING UPDATED_AT")) {
      statement.setObject(1, branch.getCode());
      statement.setObject(2, branch.getCount());
      statement.setObject(3, branch.getOpen());
      statement.setObject(4, branch.getOpenedAt());
      statement.setObject(5, offset(branch.getSeenAt()));
      statement.setObject(6, branch.getToken());
      statement.setObject(7, branch.getRatio());
      statement.setObject(8, branch.getAmount());
      statement.setObject(9, branch.getMemo());
      statement.setObject(10, branch.getParentId());
      statement.setObject(11, branch.getId());
      try (ResultSet rows = statement.executeQuery()) {
        if (!rows.next()) {
          return false;
        }
        branch.setUpdatedAt(rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant());
        return true;
      }
    }
  }

  /**
   * Deletes the row of the branch with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM BRANCH WHERE BRANCH_KEY = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  /** Deletes every branch. */
  public void deleteAll() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("DELETE FROM BRANCH")) {
      statement.executeUpdate();
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The branch of the row, which has the columns of {@link #COLUMNS}. */
  private static Branch read(ResultSet rows) throws SQLException {
    Branch branch = new Branch();
    branch.setId(rows.getObject("BRANCH_KEY", Long.class));
    branch.setCode(rows.getObject("CODE", String.class));
    branch.setCount(rows.getObject("COUNT", Long.class));
    branch.setOpen(rows.getObject("OPEN", Boolean.class));
    branch.setOpenedAt(rows.getObject("OPENED_AT", LocalDateTime.class));
    branch.setSeenAt(instant(rows.getObject("SEEN_AT", OffsetDateTime.class)));
    branch.setToken(rows.getObject("TOKEN", UUID.class));
    branch.setRatio(rows.getObject("RATIO", BigDecimal.class));
    branch.setAmount(rows.getObject("AMOUNT", BigDecimal.class));
    branch.setMemo(rows.getObject("MEMO", String.class));
    branch.setParentId(rows.getObject("PARENT_ID", Long.class));
    branch.setCreatedAt(rows.getObject("CREATED_AT", OffsetDateTime.class).toInstant());
    branch.setUpdatedAt(rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant());
    return branch;
  }

  /** The time as the driver binds it to a TIMESTAMP WITH TIME ZONE. */
  private static OffsetDateTime offset(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  /** The time of a TIMESTAMP WITH TIME ZONE as the driver reads it. */
  private static Instant instant(OffsetDateTime time) {
    return time == null ? null : time.toInstant();
  }
}
