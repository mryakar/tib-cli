// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of vehicles, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTests {

  private static final Long ID = 1L;

  @Mock private Transactions transactions;
  @Mock private VehicleRepository repository;
  @Mock private VehicleMapper mapper;
  @InjectMocks private VehicleService service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any()))
        .thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void getsAVehicle() throws SQLException {
    Vehicle vehicle = new Vehicle();
    VehicleDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(vehicle));
    when(mapper.toDto(vehicle)).thenReturn(dto);

    assertSame(dto, service.get(ID));
  }

  @Test
  void anUnknownVehicleIsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ID));
  }

  @Test
  void createsAVehicleFromItsDto() throws SQLException {
    VehicleDto dto = dto();
    when(repository.insert(any(Vehicle.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Vehicle.class))).thenReturn(dto);

    assertSame(dto, service.create(dto));
    verify(mapper).update(eq(dto), any(Vehicle.class));
  }

  @Test
  void updatesAVehicleFromItsDto() throws SQLException {
    Vehicle vehicle = new Vehicle();
    VehicleDto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(vehicle));
    when(repository.update(vehicle)).thenReturn(true);
    when(mapper.toDto(vehicle)).thenReturn(dto);

    assertSame(dto, service.update(ID, dto));
    verify(mapper).update(dto, vehicle);
  }

  @Test
  void updatingAVehicleThatIsGoneIsNotFound() throws SQLException {
    Vehicle vehicle = new Vehicle();
    when(repository.findById(ID)).thenReturn(Optional.of(vehicle));
    when(repository.update(vehicle)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.update(ID, dto()));
  }

  @Test
  void deletesAVehicle() throws SQLException {
    when(repository.delete(ID)).thenReturn(true);

    service.delete(ID);

    verify(repository).delete(ID);
  }

  @Test
  void deletingAnUnknownVehicleIsNotFound() throws SQLException {
    when(repository.delete(ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.delete(ID));
  }

  private static VehicleDto dto() {
    return VehicleSamples.dto().id(ID).build();
  }
}
