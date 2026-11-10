// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.Branch;
import me.yakar.cornerservice.branch.BranchRepository;
import me.yakar.cornerservice.branch.BranchSamples;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/** The API of items, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ItemApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private ItemRepository items;
  @Autowired private BranchRepository branches;

  private Branch branch;

  @BeforeEach
  void saveWhatAnItemNeeds() {
    branch = branches.saveAndFlush(BranchSamples.branch().build());
  }

  @AfterEach
  void deleteAll() {
    items.deleteAll();
    branches.deleteAll();
  }

  @Test
  void createsAnItem() {
    ItemDto dto = dto();

    ResponseEntity<ItemDto> created = http.postForEntity("/api/items", dto, ItemDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().id()).isNotNull();
    assertThat(created.getBody()).usingRecursiveComparison().ignoringFields("id").isEqualTo(dto);
  }

  @Test
  void getsAnItem() {
    ItemDto item = create();

    ResponseEntity<ItemDto> found = http.getForEntity("/api/items/{id}", ItemDto.class, item.id());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody()).isEqualTo(item);
  }

  @Test
  void anUnknownItemIsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("/api/items/{id}", String.class, Long.MAX_VALUE);

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updatesAnItem() {
    ItemDto item = create();
    ItemDto dto = otherDto();

    ResponseEntity<ItemDto> updated =
        http.exchange(
            "/api/items/{id}", HttpMethod.PUT, new HttpEntity<>(dto), ItemDto.class, item.id());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(item.id());
    assertThat(updated.getBody()).usingRecursiveComparison().ignoringFields("id").isEqualTo(dto);
  }

  @Test
  void listsItems() {
    ItemDto item = create();

    ResponseEntity<JsonNode> list = http.getForEntity("/api/items", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsString("id")).contains(item.id().toString());
  }

  @Test
  void deletesAnItem() {
    ItemDto item = create();

    ResponseEntity<Void> deleted =
        http.exchange("/api/items/{id}", HttpMethod.DELETE, null, Void.class, item.id());
    ResponseEntity<String> found = http.getForEntity("/api/items/{id}", String.class, item.id());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesAnItemWithoutItsRequiredValues() {
    ItemDto empty = ItemSamples.emptyDto().build();

    ResponseEntity<String> created = http.postForEntity("/api/items", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** An item created through the API, with the first sample values. */
  private ItemDto create() {
    return http.postForObject("/api/items", dto(), ItemDto.class);
  }

  /** An item as the API takes it, with the first sample values. */
  private ItemDto dto() {
    return ItemSamples.dto(branch.getId()).build();
  }

  /** An item as the API takes it, with the second sample values. */
  private ItemDto otherDto() {
    return ItemSamples.otherDto(branch.getId()).build();
  }
}
