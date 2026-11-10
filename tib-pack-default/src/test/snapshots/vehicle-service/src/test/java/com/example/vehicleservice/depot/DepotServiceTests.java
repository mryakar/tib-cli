// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of depots, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class DepotServiceTests {

  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private DepotRepository depots;
  @Mock private DepotMapper mapper;
  @InjectMocks private DepotService service;

  @Test
  void getsADepot() {
    Depot depot = new Depot();
    DepotDto dto = dto();
    when(depots.findById(ID)).thenReturn(Optional.of(depot));
    when(mapper.toDto(depot)).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknownDepotIsNotFound() {
    when(depots.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void createsADepotFromItsDto() {
    DepotDto dto = dto();
    when(depots.saveAndFlush(any(Depot.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Depot.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(Depot.class));
  }

  @Test
  void updatesADepotFromItsDto() {
    Depot depot = new Depot();
    DepotDto dto = dto();
    when(depots.findById(ID)).thenReturn(Optional.of(depot));
    when(depots.saveAndFlush(depot)).thenReturn(depot);
    when(mapper.toDto(depot)).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, depot);
  }

  @Test
  void deletesADepot() {
    Depot depot = new Depot();
    when(depots.findById(ID)).thenReturn(Optional.of(depot));

    service.delete(ID);

    verify(depots).delete(depot);
  }

  private static DepotDto dto() {
    return new DepotDto(ID, "One", LocalDate.of(2026, 1, 1));
  }
}
