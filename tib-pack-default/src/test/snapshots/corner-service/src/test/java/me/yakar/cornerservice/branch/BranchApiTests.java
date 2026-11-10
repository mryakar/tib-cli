// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import static org.assertj.core.api.Assertions.assertThat;

import me.yakar.cornerservice.TestcontainersConfiguration;
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

/** The API of branches, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class BranchApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private BranchRepository branches;

  @AfterEach
  void deleteAll() {
    branches.deleteAll();
  }

  @Test
  void createsABranch() {
    BranchDto dto = dto();

    ResponseEntity<BranchDto> created = http.postForEntity("/api/branches", dto, BranchDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().id()).isNotNull();
    assertThat(created.getBody())
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(dto);
  }

  @Test
  void getsABranch() {
    BranchDto branch = create();

    ResponseEntity<BranchDto> found =
        http.getForEntity("/api/branches/{id}", BranchDto.class, branch.id());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody())
        .usingRecursiveComparison()
        .ignoringFields("createdAt", "updatedAt")
        .isEqualTo(branch);
  }

  @Test
  void anUnknownBranchIsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("/api/branches/{id}", String.class, Long.MAX_VALUE);

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updatesABranch() {
    BranchDto branch = create();
    BranchDto dto = BranchSamples.otherDto().build();

    ResponseEntity<BranchDto> updated =
        http.exchange(
            "/api/branches/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(dto),
            BranchDto.class,
            branch.id());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(branch.id());
    assertThat(updated.getBody())
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(dto);
  }

  @Test
  void listsBranches() {
    BranchDto branch = create();

    ResponseEntity<JsonNode> list = http.getForEntity("/api/branches", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsString("id")).contains(branch.id().toString());
  }

  @Test
  void deletesABranch() {
    BranchDto branch = create();

    ResponseEntity<Void> deleted =
        http.exchange("/api/branches/{id}", HttpMethod.DELETE, null, Void.class, branch.id());
    ResponseEntity<String> found =
        http.getForEntity("/api/branches/{id}", String.class, branch.id());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesABranchWithoutItsRequiredValues() {
    BranchDto empty = BranchSamples.emptyDto().build();

    ResponseEntity<String> created = http.postForEntity("/api/branches", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A branch created through the API, with the first sample values. */
  private BranchDto create() {
    return http.postForObject("/api/branches", dto(), BranchDto.class);
  }

  /** A branch as the API takes it, with the first sample values. */
  private BranchDto dto() {
    return BranchSamples.dto().build();
  }
}
