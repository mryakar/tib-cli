// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item.adapter.in.web;

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
import me.yakar.cornerservice.branch.domain.Branch;
import me.yakar.cornerservice.item.ItemSamples;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The API of items, over HTTP, against the service and PostgreSQL in a container. */
class ItemApiTests {

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

  private Branch branch;

  @BeforeEach
  void saveWhatAnItemNeeds() {
    branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));
  }

  @AfterEach
  void deleteAll() {
    TestDatabase.clear("ITEM", "BRANCH");
  }

  @Test
  void createsAnItem() throws Exception {
    ItemDto dto = dto();

    HttpResponse<String> created = service.send("POST", "/api/items", dto);

    assertEquals(201, created.statusCode());
    ItemDto body = TestService.read(created, ItemDto.class);
    assertNotNull(body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void getsAnItem() throws Exception {
    ItemDto item = create();

    HttpResponse<String> found = service.send("GET", "/api/items/" + item.id());

    assertEquals(200, found.statusCode());
    assertEquals(item, TestService.read(found, ItemDto.class));
  }

  @Test
  void anUnknownItemIsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "/api/items/" + Long.MAX_VALUE);

    assertEquals(404, found.statusCode());
  }

  @Test
  void updatesAnItem() throws Exception {
    ItemDto item = create();
    ItemDto dto = ItemSamples.otherDto(branch.getId()).build();

    HttpResponse<String> updated = service.send("PUT", "/api/items/" + item.id(), dto);

    assertEquals(200, updated.statusCode());
    ItemDto body = TestService.read(updated, ItemDto.class);
    assertEquals(item.id(), body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void listsItems() throws Exception {
    ItemDto item = create();

    HttpResponse<String> list = service.send("GET", "/api/items");

    assertEquals(200, list.statusCode());
    JsonNode body = Json.MAPPER.readTree(list.body());
    assertEquals(0, body.path("page").path("number").asInt());
    assertEquals(20, body.path("page").path("size").asInt());
    assertTrue(body.findValuesAsString("id").contains(item.id().toString()));
  }

  @Test
  void deletesAnItem() throws Exception {
    ItemDto item = create();

    HttpResponse<String> deleted = service.send("DELETE", "/api/items/" + item.id());
    HttpResponse<String> found = service.send("GET", "/api/items/" + item.id());

    assertEquals(204, deleted.statusCode());
    assertEquals(404, found.statusCode());
  }

  @Test
  void refusesAnItemWithoutItsRequiredValues() throws Exception {
    ItemDto empty = ItemSamples.emptyDto().build();

    HttpResponse<String> created = service.send("POST", "/api/items", empty);

    assertEquals(400, created.statusCode());
  }

  /** An item created through the API, with the first sample values. */
  private ItemDto create() throws Exception {
    return TestService.read(service.send("POST", "/api/items", dto()), ItemDto.class);
  }

  /** An item as the API takes it, with the first sample values. */
  private ItemDto dto() {
    return ItemSamples.dto(branch.getId()).build();
  }

  /** The DTO without what the service gives it: its identifier. */
  private static ItemDto writable(ItemDto dto) {
    return new ItemDto(null, dto.label(), dto.item(), dto.branchId());
  }
}
