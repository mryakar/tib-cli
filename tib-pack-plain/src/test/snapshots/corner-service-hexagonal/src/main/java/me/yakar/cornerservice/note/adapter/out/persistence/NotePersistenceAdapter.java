// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.note.adapter.out.persistence;

import java.util.Optional;
import me.yakar.cornerservice.Page;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.note.application.port.out.NoteStore;
import me.yakar.cornerservice.note.domain.Note;

/**
 * The store of notes on their table, through the repository, whose SQL follows the spec. Every call
 * joins the transaction of the use case that makes it, and a failure of the database comes out of
 * it as a {@link Transactions.DatabaseException}: the store knows no JDBC.
 */
public class NotePersistenceAdapter implements NoteStore {

  private final Transactions transactions;
  private final NoteRepository repository;

  public NotePersistenceAdapter(Transactions transactions, NoteRepository repository) {
    this.transactions = transactions;
    this.repository = repository;
  }

  @Override
  public Page<Note> findAll(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size));
  }

  @Override
  public Optional<Note> findById(Long id) {
    return transactions.run(() -> repository.findById(id));
  }

  @Override
  public Note insert(Note note) {
    return transactions.run(() -> repository.insert(note));
  }

  @Override
  public boolean update(Note note) {
    return transactions.run(() -> repository.update(note));
  }

  @Override
  public boolean delete(Long id) {
    return transactions.run(() -> repository.delete(id));
  }
}
