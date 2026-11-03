// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of branches, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class BranchServiceTests {

  private static final Long ID = 1L;

  @Mock private BranchRepository branches;
  @Mock private BranchMapper mapper;
  @InjectMocks private BranchService service;

  @Test
  void getsABranch() {
    Branch branch = new Branch();
    BranchDto dto = dto();
    when(branches.findById(ID)).thenReturn(Optional.of(branch));
    when(mapper.toDto(branch)).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknownBranchIsNotFound() {
    when(branches.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void createsABranchFromItsDto() {
    BranchDto dto = dto();
    when(branches.saveAndFlush(any(Branch.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Branch.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(Branch.class));
  }

  @Test
  void updatesABranchFromItsDto() {
    Branch branch = new Branch();
    BranchDto dto = dto();
    when(branches.findById(ID)).thenReturn(Optional.of(branch));
    when(branches.saveAndFlush(branch)).thenReturn(branch);
    when(mapper.toDto(branch)).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, branch);
  }

  @Test
  void deletesABranch() {
    Branch branch = new Branch();
    when(branches.findById(ID)).thenReturn(Optional.of(branch));

    service.delete(ID);

    verify(branches).delete(branch);
  }

  private static BranchDto dto() {
    return new BranchDto(
        ID,
        "One",
        1L,
        true,
        LocalDateTime.of(2026, 1, 1, 12, 0),
        Instant.parse("2026-01-01T12:00:00Z"),
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        new BigDecimal("1"),
        new BigDecimal("1"),
        1L,
        null,
        null);
  }
}
