// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.branch.domain.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of branches, with the repository mocked. */
@ExtendWith(MockitoExtension.class)
class BranchServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private BranchRepository repository;
  @InjectMocks private BranchService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsABranch() throws SQLException {
    Branch branch = new Branch();
    when(repository.findById(ID)).thenReturn(Optional.of(branch));

    assertSame(branch, service.get(ID));
  }

  @Test
  void anUnknownBranchIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException e = assertThrows(NotFoundException.class, () -> service.get(ID));
    assertEquals("There is no branch " + ID, e.getMessage());
  }

  @Test
  void createsABranch() throws SQLException {
    Branch branch = new Branch();
    when(repository.insert(branch)).thenReturn(branch);

    assertSame(branch, service.create(branch));
  }

  @Test
  void updatesABranchAsTheChangeDoes() throws SQLException {
    Branch branch = new Branch();
    AtomicReference<Branch> changed = new AtomicReference<>();
    when(repository.findById(ID)).thenReturn(Optional.of(branch));
    when(repository.update(branch)).thenReturn(true);

    assertSame(branch, service.update(ID, changed::set));
    assertSame(branch, changed.get());
  }

  @Test
  void updatingABranchThatIsGoneIsNotFound() throws SQLException {
    Branch branch = new Branch();
    when(repository.findById(ID)).thenReturn(Optional.of(branch));
    when(repository.update(branch)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.update(ID, change -> {}));
  }

  @Test
  void deletesABranch() throws SQLException {
    when(repository.delete(ID)).thenReturn(true);

    service.delete(ID);

    verify(repository).delete(ID);
  }

  @Test
  void deletingAnUnknownBranchIsNotFound() throws SQLException {
    when(repository.delete(ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.delete(ID));
  }
}
