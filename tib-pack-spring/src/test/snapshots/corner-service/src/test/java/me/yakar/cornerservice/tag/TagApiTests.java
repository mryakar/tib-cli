// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

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
    branch = branches.saveAndFlush(BranchSamples.branch().build());
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
    assertThat(created.getBody()).usingRecursiveComparison().ignoringFields("id").isEqualTo(dto);
  }

  @Test
  void getsATag() {
    TagDto tag = create();

    ResponseEntity<TagDto> found = http.getForEntity("/api/tags/{id}", TagDto.class, tag.id());

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
    TagDto dto = TagSamples.otherDto(branch.getId()).build();

    ResponseEntity<TagDto> updated =
        http.exchange(
            "/api/tags/{id}", HttpMethod.PUT, new HttpEntity<>(dto), TagDto.class, tag.id());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(tag.id());
    assertThat(updated.getBody()).usingRecursiveComparison().ignoringFields("id").isEqualTo(dto);
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
    ResponseEntity<String> found = http.getForEntity("/api/tags/{id}", String.class, tag.id());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void refusesATagWithoutItsRequiredValues() {
    TagDto empty = TagSamples.emptyDto().build();

    ResponseEntity<String> created = http.postForEntity("/api/tags", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A tag created through the API, with the first sample values. */
  private TagDto create() {
    return http.postForObject("/api/tags", dto(), TagDto.class);
  }

  /** A tag as the API takes it, with the first sample values. */
  private TagDto dto() {
    return TagSamples.dto(branch.getId()).build();
  }
}
