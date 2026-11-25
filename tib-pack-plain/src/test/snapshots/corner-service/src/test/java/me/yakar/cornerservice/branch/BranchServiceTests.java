// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of branches, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class BranchServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private BranchRepository repository;
  @Mock private BranchMapper mapper;
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
    BranchDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(branch));
    when(mapper.toDto(branch)).thenReturn(dto);

    assertSame(dto, service.get(ID));
  }

  @Test
  void anUnknownBranchIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ID));
  }

  @Test
  void createsABranchFromItsDto() throws SQLException {
    BranchDto dto = dto();
    when(repository.insert(any(Branch.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Branch.class))).thenReturn(dto);

    assertSame(dto, service.create(dto));
    verify(mapper).update(eq(dto), any(Branch.class));
  }

  @Test
  void updatesABranchFromItsDto() throws SQLException {
    Branch branch = new Branch();
    BranchDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(branch));
    when(repository.update(branch)).thenReturn(true);
    when(mapper.toDto(branch)).thenReturn(dto);

    assertSame(dto, service.update(ID, dto));
    verify(mapper).update(dto, branch);
  }

  @Test
  void updatingABranchThatIsGoneIsNotFound() throws SQLException {
    Branch branch = new Branch();
    when(repository.findById(ID)).thenReturn(Optional.of(branch));
    when(repository.update(branch)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.update(ID, dto()));
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

  private static BranchDto dto() {
    return BranchSamples.dto().id(ID).build();
  }
}
