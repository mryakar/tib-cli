// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.vehicleservice.NotFoundException;
import com.example.vehicleservice.Transactions;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of depots, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class DepotServiceTests {

  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private Transactions transactions;
  @Mock private DepotRepository repository;
  @Mock private DepotMapper mapper;
  @InjectMocks private DepotService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsADepot() throws SQLException {
    Depot depot = new Depot();
    DepotDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(depot));
    when(mapper.toDto(depot)).thenReturn(dto);

    assertSame(dto, service.get(ID));
  }

  @Test
  void anUnknownDepotIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ID));
  }

  @Test
  void createsADepotFromItsDto() throws SQLException {
    DepotDto dto = dto();
    when(repository.insert(any(Depot.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Depot.class))).thenReturn(dto);

    assertSame(dto, service.create(dto));
    verify(mapper).update(eq(dto), any(Depot.class));
  }

  @Test
  void updatesADepotFromItsDto() throws SQLException {
    Depot depot = new Depot();
    DepotDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(depot));
    when(repository.update(depot)).thenReturn(true);
    when(mapper.toDto(depot)).thenReturn(dto);

    assertSame(dto, service.update(ID, dto));
    verify(mapper).update(dto, depot);
  }

  @Test
  void updatingADepotThatIsGoneIsNotFound() throws SQLException {
    Depot depot = new Depot();
    when(repository.findById(ID)).thenReturn(Optional.of(depot));
    when(repository.update(depot)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.update(ID, dto()));
  }

  @Test
  void deletesADepot() throws SQLException {
    when(repository.delete(ID)).thenReturn(true);

    service.delete(ID);

    verify(repository).delete(ID);
  }

  @Test
  void deletingAnUnknownDepotIsNotFound() throws SQLException {
    when(repository.delete(ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.delete(ID));
  }

  private static DepotDto dto() {
    return DepotSamples.dto().id(ID).build();
  }
}
