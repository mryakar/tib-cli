// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.vehicleservice.Json;
import com.example.vehicleservice.TestDatabase;
import com.example.vehicleservice.TestService;
import com.example.vehicleservice.Transactions;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The API of vehicles, over HTTP, against the service and PostgreSQL in a container. */
class VehicleApiTests {

  private static TestService service;

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final VehicleRepository vehicleRepository = new VehicleRepository(transactions);

  @BeforeAll
  static void start() throws IOException {
    service = TestService.start();
  }

  @AfterAll
  static void stop() {
    service.close();
  }

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          vehicleRepository.deleteAll();
          return null;
        });
  }

  @Test
  void createsAVehicle() throws Exception {
    VehicleDto dto = dto();

    HttpResponse<String> created = service.send("POST", "/api/vehicles", dto);

    assertEquals(201, created.statusCode());
    VehicleDto body = TestService.read(created, VehicleDto.class);
    assertNotNull(body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void getsAVehicle() throws Exception {
    VehicleDto vehicle = create();

    HttpResponse<String> found = service.send("GET", "/api/vehicles/" + vehicle.id());

    assertEquals(200, found.statusCode());
    assertEquals(vehicle, TestService.read(found, VehicleDto.class));
  }

  @Test
  void anUnknownVehicleIsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "/api/vehicles/" + Long.MAX_VALUE);

    assertEquals(404, found.statusCode());
  }

  @Test
  void updatesAVehicle() throws Exception {
    VehicleDto vehicle = create();
    VehicleDto dto = VehicleSamples.otherDto().build();

    HttpResponse<String> updated = service.send("PUT", "/api/vehicles/" + vehicle.id(), dto);

    assertEquals(200, updated.statusCode());
    VehicleDto body = TestService.read(updated, VehicleDto.class);
    assertEquals(vehicle.id(), body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void listsVehicles() throws Exception {
    VehicleDto vehicle = create();

    HttpResponse<String> list = service.send("GET", "/api/vehicles");

    assertEquals(200, list.statusCode());
    JsonNode body = Json.MAPPER.readTree(list.body());
    assertEquals(0, body.path("page").path("number").asInt());
    assertEquals(20, body.path("page").path("size").asInt());
    assertTrue(body.findValuesAsString("id").contains(vehicle.id().toString()));
  }

  @Test
  void deletesAVehicle() throws Exception {
    VehicleDto vehicle = create();

    HttpResponse<String> deleted = service.send("DELETE", "/api/vehicles/" + vehicle.id());
    HttpResponse<String> found = service.send("GET", "/api/vehicles/" + vehicle.id());

    assertEquals(204, deleted.statusCode());
    assertEquals(404, found.statusCode());
  }

  @Test
  void refusesAVehicleWithoutItsRequiredValues() throws Exception {
    VehicleDto empty = VehicleSamples.emptyDto().build();

    HttpResponse<String> created = service.send("POST", "/api/vehicles", empty);

    assertEquals(400, created.statusCode());
  }

  @Test
  void refusesAVehicleWhoseValueDoesNotFitItsColumn() throws Exception {
    VehicleDto tooLarge = VehicleSamples.dto().price(BigDecimal.TEN.pow(10)).build();

    HttpResponse<String> created = service.send("POST", "/api/vehicles", tooLarge);

    assertEquals(409, created.statusCode());
  }

  @Test
  void refusesAVehicleWhoseUniqueValuesAreTaken() throws Exception {
    create();

    HttpResponse<String> created = service.send("POST", "/api/vehicles", dto());

    assertEquals(409, created.statusCode());
    assertEquals(
        "The request breaks a constraint of the database.",
        Json.MAPPER.readTree(created.body()).path("detail").asString());
  }

  /** A vehicle created through the API, with the first sample values. */
  private VehicleDto create() throws Exception {
    return TestService.read(service.send("POST", "/api/vehicles", dto()), VehicleDto.class);
  }

  /** A vehicle as the API takes it, with the first sample values. */
  private VehicleDto dto() {
    return VehicleSamples.dto().build();
  }

  /** The DTO without what the service gives it: its identifier and its audit times. */
  private static VehicleDto writable(VehicleDto dto) {
    return new VehicleDto(
        null, dto.plateNumber(), dto.capacity(), dto.price(), dto.depotId(), null, null);
  }
}
