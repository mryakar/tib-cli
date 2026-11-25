@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@import me.yakar.tib.pack.Sources
@import me.yakar.tib.pack.spring.JpaModel
@param EntityTestsModel tests
@param JpaModel jpa
!{EntityModel entity = tests.entity();}
!{String path = entity.path();}
!{String dto = entity.name() + "Dto";}
package ${entity.webPackage()};

import static org.assertj.core.api.Assertions.assertThat;

import ${entity.basePackage()}.TestcontainersConfiguration;
${Sources.importOf(entity.webPackage(), entity.packageName(), entity.name() + "Samples")}
@for(var target : tests.requiredTargets())
${Sources.importOf(entity.webPackage(), target.domainPackage(), target.name())}
${Sources.importOf(entity.webPackage(), target.persistencePackage(), jpa.repository(target))}
@if(entity.hexagonal())
import ${target.storePackage()}.${target.name()}Store;
@endif
${Sources.importOf(entity.webPackage(), target.packageName(), target.name() + "Samples")}
@endfor
${Sources.importOf(entity.webPackage(), entity.persistencePackage(), jpa.repository())}
@if(entity.uuidId())
import java.util.UUID;
@endif
import org.junit.jupiter.api.AfterEach;
@if(!tests.requiredTargets().isEmpty())
import org.junit.jupiter.api.BeforeEach;
@endif
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

/** The API of ${entity.plural()}, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class ${entity.name()}ApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private ${jpa.repository()} ${tests.repositoryVariable(entity)};
  @for(var target : tests.requiredTargets())
  @if(entity.hexagonal())
  @Autowired private ${target.name()}Store ${target.plural()};
  @endif
  @Autowired private ${jpa.repository(target)} ${tests.repositoryVariable(target)};
  @endfor
  @template.entity.required_targets(tests = tests)

  @AfterEach
  void deleteAll() {
    ${tests.repositoryVariable(entity)}.deleteAll();
    @for(var target : tests.requiredTargets().reversed())
    ${tests.repositoryVariable(target)}.deleteAll();
    @endfor
  }

  @Test
  void creates${entity.article()}${entity.name()}() {
    ${dto} dto = dto();

    ResponseEntity<${dto}> created = http.postForEntity("${path}", dto, ${dto}.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().${entity.idField()}()).isNotNull();
    assertThat(created.getBody())
        .usingRecursiveComparison()
        .ignoringFields(${tests.ignoredFields()})
        .isEqualTo(dto);
  }

  @Test
  void gets${entity.article()}${entity.name()}() {
    ${dto} ${entity.variable()} = create();

    ResponseEntity<${dto}> found =
        http.getForEntity("${path}/{id}", ${dto}.class, ${entity.variable()}.${entity.idField()}());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    @if(entity.audited())
    assertThat(found.getBody())
        .usingRecursiveComparison()
        .ignoringFields("createdAt", "updatedAt")
        .isEqualTo(${entity.variable()});
    @else
    assertThat(found.getBody()).isEqualTo(${entity.variable()});
    @endif
  }

  @Test
  void anUnknown${entity.name()}IsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("${path}/{id}", String.class, ${tests.unknownId()});

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updates${entity.article()}${entity.name()}() {
    ${dto} ${entity.variable()} = create();
    ${dto} dto = ${entity.name()}Samples.otherDto(${tests.requiredIdArguments()}).build();

    ResponseEntity<${dto}> updated =
        http.exchange(
            "${path}/{id}",
            HttpMethod.PUT,
            new HttpEntity<>(dto),
            ${dto}.class,
            ${entity.variable()}.${entity.idField()}());

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().${entity.idField()}()).isEqualTo(${entity.variable()}.${entity.idField()}());
    assertThat(updated.getBody())
        .usingRecursiveComparison()
        .ignoringFields(${tests.ignoredFields()})
        .isEqualTo(dto);
  }

  @Test
  void lists${tests.pluralType()}() {
    ${dto} ${entity.variable()} = create();

    ResponseEntity<JsonNode> list = http.getForEntity("${path}", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsString("${entity.idField()}")).contains(${entity.variable()}.${entity.idField()}().toString());
  }

  @Test
  void deletes${entity.article()}${entity.name()}() {
    ${dto} ${entity.variable()} = create();

    ResponseEntity<Void> deleted =
        http.exchange("${path}/{id}", HttpMethod.DELETE, null, Void.class, ${entity.variable()}.${entity.idField()}());
    ResponseEntity<String> found =
        http.getForEntity("${path}/{id}", String.class, ${entity.variable()}.${entity.idField()}());

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
  @if(tests.requiresValues())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${entity.name()}WithoutItsRequiredValues() {
    ${dto} empty = ${entity.name()}Samples.emptyDto().build();

    ResponseEntity<String> created = http.postForEntity("${path}", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
  @endif
  @if(tests.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${entity.name()}WhoseUniqueValuesAreTaken() {
    create();

    ResponseEntity<JsonNode> created = http.postForEntity("${path}", dto(), JsonNode.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(created.getBody().path("detail").asString())
        .isEqualTo("The request breaks a constraint of the database.");
  }
  @endif<%-- keeps the blank line below --%>

  /** ${entity.article()} ${entity.variable()} created through the API, with the first sample values. */
  private ${dto} create() {
    return http.postForObject("${path}", dto(), ${dto}.class);
  }

  /** ${entity.article()} ${entity.variable()} as the API takes it, with the first sample values. */
  private ${dto} dto() {
    return ${entity.name()}Samples.dto(${tests.requiredIdArguments()}).build();
  }
}
