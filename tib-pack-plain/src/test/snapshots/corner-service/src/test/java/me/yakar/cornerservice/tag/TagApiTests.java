// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpResponse;
import me.yakar.cornerservice.Json;
import me.yakar.cornerservice.TestDatabase;
import me.yakar.cornerservice.TestService;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.Branch;
import me.yakar.cornerservice.branch.BranchRepository;
import me.yakar.cornerservice.branch.BranchSamples;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The API of tags, over HTTP, against the service and PostgreSQL in a container. */
class TagApiTests {

  private static TestService service;

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final BranchRepository branchRepository = new BranchRepository(transactions);

  private Branch branch;

  @BeforeEach
  void saveWhatATagNeeds() {
    branch = transactions.run(() -> branchRepository.insert(BranchSamples.branch().build()));
  }

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
    TestDatabase.clear("TAG", "BRANCH");
  }

  @Test
  void createsATag() throws Exception {
    TagDto dto = dto();

    HttpResponse<String> created = service.send("POST", "/api/tags", dto);

    assertEquals(201, created.statusCode());
    TagDto body = TestService.read(created, TagDto.class);
    assertNotNull(body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void getsATag() throws Exception {
    TagDto tag = create();

    HttpResponse<String> found = service.send("GET", "/api/tags/" + tag.id());

    assertEquals(200, found.statusCode());
    assertEquals(tag, TestService.read(found, TagDto.class));
  }

  @Test
  void anUnknownTagIsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "/api/tags/" + Long.MAX_VALUE);

    assertEquals(404, found.statusCode());
  }

  @Test
  void aPathThatNoEntityHasIsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "/api");

    assertEquals(404, found.statusCode());
    assertEquals(
        "There is nothing at /api", Json.MAPPER.readTree(found.body()).path("detail").asString());
  }

  @Test
  void updatesATag() throws Exception {
    TagDto tag = create();
    TagDto dto = TagSamples.otherDto(branch.getId()).build();

    HttpResponse<String> updated = service.send("PUT", "/api/tags/" + tag.id(), dto);

    assertEquals(200, updated.statusCode());
    TagDto body = TestService.read(updated, TagDto.class);
    assertEquals(tag.id(), body.id());
    assertEquals(dto, writable(body));
  }

  @Test
  void listsTags() throws Exception {
    TagDto tag = create();

    HttpResponse<String> list = service.send("GET", "/api/tags");

    assertEquals(200, list.statusCode());
    JsonNode body = Json.MAPPER.readTree(list.body());
    assertEquals(0, body.path("page").path("number").asInt());
    assertEquals(20, body.path("page").path("size").asInt());
    assertTrue(body.findValuesAsString("id").contains(tag.id().toString()));
  }

  @Test
  void deletesATag() throws Exception {
    TagDto tag = create();

    HttpResponse<String> deleted = service.send("DELETE", "/api/tags/" + tag.id());
    HttpResponse<String> found = service.send("GET", "/api/tags/" + tag.id());

    assertEquals(204, deleted.statusCode());
    assertEquals(404, found.statusCode());
  }

  @Test
  void refusesATagWithoutItsRequiredValues() throws Exception {
    TagDto empty = TagSamples.emptyDto().build();

    HttpResponse<String> created = service.send("POST", "/api/tags", empty);

    assertEquals(400, created.statusCode());
  }

  /** A tag created through the API, with the first sample values. */
  private TagDto create() throws Exception {
    return TestService.read(service.send("POST", "/api/tags", dto()), TagDto.class);
  }

  /** A tag as the API takes it, with the first sample values. */
  private TagDto dto() {
    return TagSamples.dto(branch.getId()).build();
  }

  /** The DTO without what the service gives it: its identifier. */
  private static TagDto writable(TagDto dto) {
    return new TagDto(null, dto.label(), dto.branchId(), dto.otherId());
  }
}
