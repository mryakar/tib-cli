// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpResponse;
import me.yakar.cornerservice.Json;
import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.TestService;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.BranchSamples;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The API of branches, over HTTP, against the service and PostgreSQL in a container. */
class BranchApiTests {

  private static TestService service;

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final BranchRepository branchRepository = new BranchRepository(transactions);

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
          branchRepository.deleteAll();
          return null;
        });
  }

  @Test
  void createsABranch() throws Exception {
    BranchDto dto = dto();

    HttpResponse<String> created = service.send("POST", "/api/branches", dto);

    assertEquals(201, created.statusCode());
    BranchDto body = TestService.read(created, BranchDto.class);
    assertNotNull(body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void getsABranch() throws Exception {
    BranchDto branch = create();

    HttpResponse<String> found = service.send("GET", "/api/branches/" + branch.id());

    assertEquals(200, found.statusCode());
    assertEquals(branch, TestService.read(found, BranchDto.class));
  }

  @Test
  void anUnknownBranchIsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "/api/branches/" + Long.MAX_VALUE);

    assertEquals(404, found.statusCode());
  }

  @Test
  void updatesABranch() throws Exception {
    BranchDto branch = create();
    BranchDto dto = BranchSamples.otherDto().build();

    HttpResponse<String> updated = service.send("PUT", "/api/branches/" + branch.id(), dto);

    assertEquals(200, updated.statusCode());
    BranchDto body = TestService.read(updated, BranchDto.class);
    assertEquals(branch.id(), body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void listsBranches() throws Exception {
    BranchDto branch = create();

    HttpResponse<String> list = service.send("GET", "/api/branches");

    assertEquals(200, list.statusCode());
    JsonNode body = Json.MAPPER.readTree(list.body());
    assertTrue(body.isArray());
    assertTrue(body.findValuesAsString("id").contains(branch.id().toString()));
  }

  @Test
  void deletesABranch() throws Exception {
    BranchDto branch = create();

    HttpResponse<String> deleted = service.send("DELETE", "/api/branches/" + branch.id());
    HttpResponse<String> found = service.send("GET", "/api/branches/" + branch.id());

    assertEquals(204, deleted.statusCode());
    assertEquals(404, found.statusCode());
  }

  @Test
  void refusesABranchWithoutItsRequiredValues() throws Exception {
    BranchDto empty = BranchSamples.emptyDto().build();

    HttpResponse<String> created = service.send("POST", "/api/branches", empty);

    assertEquals(400, created.statusCode());
  }

  /** A branch created through the API, with the first sample values. */
  private BranchDto create() throws Exception {
    return TestService.read(service.send("POST", "/api/branches", dto()), BranchDto.class);
  }

  /** A branch as the API takes it, with the first sample values. */
  private BranchDto dto() {
    return BranchSamples.dto().build();
  }

  /** The DTO without what the service gives it: its identifier and its audit times. */
  private static BranchDto writable(BranchDto dto) {
    return new BranchDto(
        null,
        dto.code(),
        dto.count(),
        dto.open(),
        dto.openedAt(),
        dto.seenAt(),
        dto.token(),
        dto.ratio(),
        dto.amount(),
        dto.memo(),
        dto.parentId(),
        null,
        null);
  }
}
