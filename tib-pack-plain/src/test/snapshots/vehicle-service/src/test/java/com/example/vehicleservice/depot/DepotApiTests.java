// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.vehicleservice.Json;
import com.example.vehicleservice.TestDatabase;
import com.example.vehicleservice.TestService;
import com.example.vehicleservice.Transactions;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The API of depots, over HTTP, against the service and PostgreSQL in a container. */
class DepotApiTests {

  private static TestService service;

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final DepotRepository depotRepository = new DepotRepository(transactions);

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
          depotRepository.deleteAll();
          return null;
        });
  }

  @Test
  void createsADepot() throws Exception {
    DepotDto dto = dto();

    HttpResponse<String> created = service.send("POST", "/api/depots", dto);

    assertEquals(201, created.statusCode());
    DepotDto body = TestService.read(created, DepotDto.class);
    assertNotNull(body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void getsADepot() throws Exception {
    DepotDto depot = create();

    HttpResponse<String> found = service.send("GET", "/api/depots/" + depot.id());

    assertEquals(200, found.statusCode());
    assertEquals(depot, TestService.read(found, DepotDto.class));
  }

  @Test
  void anUnknownDepotIsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "/api/depots/" + UUID.randomUUID());

    assertEquals(404, found.statusCode());
  }

  @Test
  void updatesADepot() throws Exception {
    DepotDto depot = create();
    DepotDto dto = DepotSamples.otherDto().build();

    HttpResponse<String> updated = service.send("PUT", "/api/depots/" + depot.id(), dto);

    assertEquals(200, updated.statusCode());
    DepotDto body = TestService.read(updated, DepotDto.class);
    assertEquals(depot.id(), body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void listsDepots() throws Exception {
    DepotDto depot = create();

    HttpResponse<String> list = service.send("GET", "/api/depots");

    assertEquals(200, list.statusCode());
    JsonNode body = Json.MAPPER.readTree(list.body());
    assertEquals(0, body.path("page").path("number").asInt());
    assertEquals(20, body.path("page").path("size").asInt());
    assertTrue(body.findValuesAsString("id").contains(depot.id().toString()));
  }

  @Test
  void deletesADepot() throws Exception {
    DepotDto depot = create();

    HttpResponse<String> deleted = service.send("DELETE", "/api/depots/" + depot.id());
    HttpResponse<String> found = service.send("GET", "/api/depots/" + depot.id());

    assertEquals(204, deleted.statusCode());
    assertEquals(404, found.statusCode());
  }

  @Test
  void refusesADepotWithoutItsRequiredValues() throws Exception {
    DepotDto empty = DepotSamples.emptyDto().build();

    HttpResponse<String> created = service.send("POST", "/api/depots", empty);

    assertEquals(400, created.statusCode());
  }

  @Test
  void refusesADepotWhoseUniqueValuesAreTaken() throws Exception {
    create();

    HttpResponse<String> created = service.send("POST", "/api/depots", dto());

    assertEquals(409, created.statusCode());
    assertEquals(
        "The request breaks a constraint of the database.",
        Json.MAPPER.readTree(created.body()).path("detail").asString());
  }

  /** A depot created through the API, with the first sample values. */
  private DepotDto create() throws Exception {
    return TestService.read(service.send("POST", "/api/depots", dto()), DepotDto.class);
  }

  /** A depot as the API takes it, with the first sample values. */
  private DepotDto dto() {
    return DepotSamples.dto().build();
  }

  /** The DTO without what the service gives it: its identifier. */
  private static DepotDto writable(DepotDto dto) {
    return new DepotDto(null, dto.code(), dto.openedOn());
  }
}
