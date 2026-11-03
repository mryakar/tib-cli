// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of vehicles, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTests {

  private static final Long ID = 1L;

  @Mock private VehicleRepository vehicles;
  @Mock private VehicleMapper mapper;
  @InjectMocks private VehicleService service;

  @Test
  void getsAVehicle() {
    Vehicle vehicle = new Vehicle();
    VehicleDto dto = dto();
    when(vehicles.findById(ID)).thenReturn(Optional.of(vehicle));
    when(mapper.toDto(vehicle)).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknownVehicleIsNotFound() {
    when(vehicles.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void createsAVehicleFromItsDto() {
    VehicleDto dto = dto();
    when(vehicles.saveAndFlush(any(Vehicle.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(Vehicle.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(Vehicle.class));
  }

  @Test
  void updatesAVehicleFromItsDto() {
    Vehicle vehicle = new Vehicle();
    VehicleDto dto = dto();
    when(vehicles.findById(ID)).thenReturn(Optional.of(vehicle));
    when(vehicles.saveAndFlush(vehicle)).thenReturn(vehicle);
    when(mapper.toDto(vehicle)).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, vehicle);
  }

  @Test
  void deletesAVehicle() {
    Vehicle vehicle = new Vehicle();
    when(vehicles.findById(ID)).thenReturn(Optional.of(vehicle));

    service.delete(ID);

    verify(vehicles).delete(vehicle);
  }

  private static VehicleDto dto() {
    return new VehicleDto(
        ID,
        "One",
        1,
        new BigDecimal("1.00"),
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        null,
        null);
  }
}
