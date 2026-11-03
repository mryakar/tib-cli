// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.vehicleservice.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** The API of vehicles, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class VehicleApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private VehicleRepository vehicles;

  @AfterEach
  void deleteAll() {
    vehicles.deleteAll();
  }

  @Test
  void createsAVehicle() {
    VehicleDto dto = dto();

    ResponseEntity<VehicleDto> created = http.postForEntity("/api/vehicles", dto, VehicleDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().id()).isNotNull();
    assertThat(created.getBody())
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(dto);
  }

  @Test
  void getsAVehicle() {
    VehicleDto vehicle = create();

    ResponseEntity<VehicleDto> found =
        http.getForEntity("/api/vehicles/{id}", VehicleDto.class, vehicle.id());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody())
        .usingRecursiveComparison()
        .ignoringFields("createdAt", "updatedAt")
        .isEqualTo(vehicle);
  }

  @Test
  void anUnknownVehicleIsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("/api/vehicles/{id}", String.class, Long.MAX_VALUE);

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updatesAVehicle() {
    VehicleDto vehicle = create();
    VehicleDto dto = otherDto();

    ResponseEntity<VehicleDto> updated =
        http.exchange(
            "/api/vehicles/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(dto),
            VehicleDto.class,
            vehicle.id());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(vehicle.id());
    assertThat(updated.getBody())
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(dto);
  }

  @Test
  void listsVehicles() {
    VehicleDto vehicle = create();

    ResponseEntity<JsonNode> list = http.getForEntity("/api/vehicles", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsText("id")).contains(vehicle.id().toString());
  }

  @Test
  void deletesAVehicle() {
    VehicleDto vehicle = create();

    ResponseEntity<Void> deleted =
        http.exchange("/api/vehicles/{id}", HttpMethod.DELETE, null, Void.class, vehicle.id());
    ResponseEntity<String> found =
        http.getForEntity("/api/vehicles/{id}", String.class, vehicle.id());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesAVehicleWithoutItsRequiredValues() {
    VehicleDto empty = new VehicleDto(null, null, null, null, null, null, null);

    ResponseEntity<String> created = http.postForEntity("/api/vehicles", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A vehicle created through the API, with the first sample values. */
  private VehicleDto create() {
    return http.postForObject("/api/vehicles", dto(), VehicleDto.class);
  }

  /** A vehicle as the API takes it, with the first sample values. */
  private VehicleDto dto() {
    return new VehicleDto(
        null,
        "One",
        1,
        new BigDecimal("1.00"),
        null,
        null,
        null);
  }

  /** A vehicle as the API takes it, with the second sample values. */
  private VehicleDto otherDto() {
    return new VehicleDto(
        null,
        "Two",
        2,
        new BigDecimal("2.00"),
        null,
        null,
        null);
  }
}
