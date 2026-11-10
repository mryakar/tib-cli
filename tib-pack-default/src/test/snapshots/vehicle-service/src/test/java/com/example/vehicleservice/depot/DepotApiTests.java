// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.vehicleservice.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

/** The API of depots, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class DepotApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private DepotRepository depots;

  @AfterEach
  void deleteAll() {
    depots.deleteAll();
  }

  @Test
  void createsADepot() {
    DepotDto dto = dto();

    ResponseEntity<DepotDto> created = http.postForEntity("/api/depots", dto, DepotDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().id()).isNotNull();
    assertThat(created.getBody()).usingRecursiveComparison().ignoringFields("id").isEqualTo(dto);
  }

  @Test
  void getsADepot() {
    DepotDto depot = create();

    ResponseEntity<DepotDto> found =
        http.getForEntity("/api/depots/{id}", DepotDto.class, depot.id());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody()).isEqualTo(depot);
  }

  @Test
  void anUnknownDepotIsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("/api/depots/{id}", String.class, UUID.randomUUID());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updatesADepot() {
    DepotDto depot = create();
    DepotDto dto = otherDto();

    ResponseEntity<DepotDto> updated =
        http.exchange(
            "/api/depots/{id}", HttpMethod.PUT, new HttpEntity<>(dto), DepotDto.class, depot.id());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(depot.id());
    assertThat(updated.getBody()).usingRecursiveComparison().ignoringFields("id").isEqualTo(dto);
  }

  @Test
  void listsDepots() {
    DepotDto depot = create();

    ResponseEntity<JsonNode> list = http.getForEntity("/api/depots", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsString("id")).contains(depot.id().toString());
  }

  @Test
  void deletesADepot() {
    DepotDto depot = create();

    ResponseEntity<Void> deleted =
        http.exchange("/api/depots/{id}", HttpMethod.DELETE, null, Void.class, depot.id());
    ResponseEntity<String> found = http.getForEntity("/api/depots/{id}", String.class, depot.id());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesADepotWithoutItsRequiredValues() {
    DepotDto empty = DepotSamples.emptyDto().build();

    ResponseEntity<String> created = http.postForEntity("/api/depots", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void refusesADepotWhoseUniqueValuesAreTaken() {
    create();

    ResponseEntity<JsonNode> created = http.postForEntity("/api/depots", dto(), JsonNode.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(created.getBody().path("detail").asString())
        .isEqualTo("The request breaks a constraint of the database.");
  }

  /** A depot created through the API, with the first sample values. */
  private DepotDto create() {
    return http.postForObject("/api/depots", dto(), DepotDto.class);
  }

  /** A depot as the API takes it, with the first sample values. */
  private DepotDto dto() {
    return DepotSamples.dto().build();
  }

  /** A depot as the API takes it, with the second sample values. */
  private DepotDto otherDto() {
    return DepotSamples.otherDto().build();
  }
}
