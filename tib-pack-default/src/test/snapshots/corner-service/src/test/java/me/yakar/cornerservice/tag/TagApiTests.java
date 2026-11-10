// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import me.yakar.cornerservice.TestcontainersConfiguration;
import me.yakar.cornerservice.branch.Branch;
import me.yakar.cornerservice.branch.BranchRepository;
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

/** The API of tags, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class TagApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private TagRepository tags;
  @Autowired private BranchRepository branches;

  private Branch branch;

  @BeforeEach
  void saveWhatATagNeeds() {
    branch = branches.saveAndFlush(newBranch());
  }

  @AfterEach
  void deleteAll() {
    tags.deleteAll();
    branches.deleteAll();
  }

  @Test
  void createsATag() {
    TagDto dto = dto();

    ResponseEntity<TagDto> created = http.postForEntity("/api/tags", dto, TagDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().id()).isNotNull();
    assertThat(created.getBody())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(dto);
  }

  @Test
  void getsATag() {
    TagDto tag = create();

    ResponseEntity<TagDto> found =
        http.getForEntity("/api/tags/{id}", TagDto.class, tag.id());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody()).isEqualTo(tag);
  }

  @Test
  void anUnknownTagIsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("/api/tags/{id}", String.class, Long.MAX_VALUE);

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updatesATag() {
    TagDto tag = create();
    TagDto dto = otherDto();

    ResponseEntity<TagDto> updated =
        http.exchange(
            "/api/tags/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(dto),
            TagDto.class,
            tag.id());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(tag.id());
    assertThat(updated.getBody())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(dto);
  }

  @Test
  void listsTags() {
    TagDto tag = create();

    ResponseEntity<JsonNode> list = http.getForEntity("/api/tags", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsString("id")).contains(tag.id().toString());
  }

  @Test
  void deletesATag() {
    TagDto tag = create();

    ResponseEntity<Void> deleted =
        http.exchange("/api/tags/{id}", HttpMethod.DELETE, null, Void.class, tag.id());
    ResponseEntity<String> found =
        http.getForEntity("/api/tags/{id}", String.class, tag.id());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesATagWithoutItsRequiredValues() {
    TagDto empty = new TagDto(null, null, null, null);

    ResponseEntity<String> created = http.postForEntity("/api/tags", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A tag created through the API, with the first sample values. */
  private TagDto create() {
    return http.postForObject("/api/tags", dto(), TagDto.class);
  }

  /** A tag as the API takes it, with the first sample values. */
  private TagDto dto() {
    return new TagDto(
        null,
        "One",
        branch.getId(),
        null);
  }

  /** A tag as the API takes it, with the second sample values. */
  private TagDto otherDto() {
    return new TagDto(
        null,
        "Two",
        branch.getId(),
        null);
  }

  private Branch newBranch() {
    Branch branch = new Branch();
    branch.setCode("One");
    branch.setCount(1L);
    branch.setOpen(true);
    branch.setOpenedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
    branch.setSeenAt(Instant.parse("2026-01-01T12:00:00Z"));
    branch.setToken(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    branch.setRatio(new BigDecimal("1"));
    branch.setAmount(new BigDecimal("1"));
    return branch;
  }
}
