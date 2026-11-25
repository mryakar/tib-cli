// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.yakar.cornerservice.NotFoundException;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;
import me.yakar.cornerservice.branch.domain.Branch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of branches, with the store mocked. */
@ExtendWith(MockitoExtension.class)
class BranchServiceTests {

  private static final Long ID = 1L;

  @Mock private BranchStore branches;
  @InjectMocks private BranchService service;

  @Test
  void getsABranch() {
    Branch branch = new Branch();
    when(branches.findById(ID)).thenReturn(Optional.of(branch));

    assertThat(service.get(ID)).isSameAs(branch);
  }

  @Test
  void anUnknownBranchIsNotFound() {
    when(branches.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("There is no branch " + ID);
  }

  @Test
  void createsABranch() {
    Branch branch = new Branch();
    when(branches.save(branch)).thenReturn(branch);

    assertThat(service.create(branch)).isSameAs(branch);
  }

  @Test
  void updatesABranchAsTheChangeDoes() {
    Branch branch = new Branch();
    AtomicReference<Branch> changed = new AtomicReference<>();
    when(branches.findById(ID)).thenReturn(Optional.of(branch));
    when(branches.save(branch)).thenReturn(branch);

    assertThat(service.update(ID, changed::set)).isSameAs(branch);
    assertThat(changed.get()).isSameAs(branch);
  }

  @Test
  void deletesABranch() {
    Branch branch = new Branch();
    when(branches.findById(ID)).thenReturn(Optional.of(branch));

    service.delete(ID);

    verify(branches).delete(branch);
  }
}
