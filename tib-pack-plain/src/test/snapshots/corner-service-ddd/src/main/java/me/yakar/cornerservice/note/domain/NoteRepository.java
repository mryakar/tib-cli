// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.note.domain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;

/** The table NOTE, in SQL, on the connection of the unit of work that {@link Transactions} runs. */
public class NoteRepository {

  private static final String COLUMNS = "ID, TEXT, BODY";

  private final Transactions transactions;

  public NoteRepository(Transactions transactions) {
    this.transactions = transactions;
  }

  /** The page of notes with the number, from 0, in the order of their identifiers. */
  public Page<Note> findAll(int number, int size) throws SQLException {
    List<Note> notes = new ArrayList<>();
    try (PreparedStatement statement =
        connection()
            .prepareStatement("SELECT " + COLUMNS + " FROM NOTE ORDER BY ID LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          notes.add(read(rows));
        }
      }
    }
    return Page.of(notes, number, size, count());
  }

  /** How many notes there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("SELECT COUNT(*) FROM NOTE");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<Note> findById(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM NOTE WHERE ID = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the note, which gets what the database gives its row, and gives it. */
  public Note insert(Note note) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("INSERT INTO NOTE (TEXT, BODY) VALUES (?, ?) RETURNING ID")) {
      statement.setObject(1, note.getText());
      statement.setObject(2, note.getBody());
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        note.setId(rows.getObject("ID", Long.class));
      }
    }
    return note;
  }

  /**
   * Updates the row of the note, all but its identifier.
   *
   * @return whether the table has the row
   */
  public boolean update(Note note) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("UPDATE NOTE SET TEXT = ?, BODY = ? WHERE ID = ?")) {
      statement.setObject(1, note.getText());
      statement.setObject(2, note.getBody());
      statement.setObject(3, note.getId());
      return statement.executeUpdate() == 1;
    }
  }

  /**
   * Deletes the row of the note with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(Long id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("DELETE FROM NOTE WHERE ID = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The note of the row, which has the columns of {@link #COLUMNS}. */
  private static Note read(ResultSet rows) throws SQLException {
    Note note = new Note();
    note.setId(rows.getObject("ID", Long.class));
    note.setText(rows.getObject("TEXT", String.class));
    note.setBody(rows.getObject("BODY", String.class));
    return note;
  }
}
