@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@import me.yakar.tib.pack.plain.DtoModel
@param EntityTestsModel tests
@param DtoModel dto
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String path = entity.path();}
!{String type = name + "Dto";}
!{String repository = entity.variable() + "Repository";}
package ${entity.webPackage()};

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ${entity.basePackage()}.Json;
import ${entity.basePackage()}.TestDatabase;
import ${entity.basePackage()}.TestService;
import ${entity.basePackage()}.Transactions;
${EntityModel.importOf(entity.webPackage(), entity.packageName(), name + "Samples")}
${EntityModel.importOf(entity.webPackage(), entity.persistencePackage(), name + "Repository")}
@for(var target : tests.requiredTargets())
${EntityModel.importOf(entity.webPackage(), target.domainPackage(), target.name())}
${EntityModel.importOf(entity.webPackage(), target.persistencePackage(), target.name() + "Repository")}
${EntityModel.importOf(entity.webPackage(), target.packageName(), target.name() + "Samples")}
@endfor
import java.io.IOException;
import java.net.http.HttpResponse;
@if(entity.uuidId())
import java.util.UUID;
@endif
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
@if(!tests.requiredTargets().isEmpty())
import org.junit.jupiter.api.BeforeEach;
@endif
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The API of ${entity.plural()}, over HTTP, against the service and PostgreSQL in a container. */
class ${name}ApiTests {

  private static TestService service;

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final ${name}Repository ${repository} = new ${name}Repository(transactions);
  @for(var target : tests.requiredTargets())
  private final ${target.name()}Repository ${target.variable()}Repository = new ${target.name()}Repository(transactions);
  @endfor

  @BeforeAll
  static void start() throws IOException {
    service = TestService.start();
  }

  @AfterAll
  static void stop() {
    service.close();
  }
  @template.entity.required_targets(tests = tests)

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          ${repository}.deleteAll();
          @for(var target : tests.requiredTargets().reversed())
          ${target.variable()}Repository.deleteAll();
          @endfor
          return null;
        });
  }

  @Test
  void creates${entity.article()}${name}() throws Exception {
    ${type} dto = dto();

    HttpResponse<String> created = service.send("POST", "${path}", dto);

    assertEquals(201, created.statusCode());
    ${type} body = TestService.read(created, ${type}.class);
    assertNotNull(body.${entity.idField()}());
    assertEquals(dto, writable(body));
  }

  @Test
  void gets${entity.article()}${name}() throws Exception {
    ${type} ${entity.variable()} = create();

    HttpResponse<String> found = service.send("GET", "${path}/" + ${entity.variable()}.${entity.idField()}());

    assertEquals(200, found.statusCode());
    assertEquals(${entity.variable()}, TestService.read(found, ${type}.class));
  }

  @Test
  void anUnknown${name}IsNotFound() throws Exception {
    HttpResponse<String> found = service.send("GET", "${path}/" + ${tests.unknownId()});

    assertEquals(404, found.statusCode());
  }

  @Test
  void updates${entity.article()}${name}() throws Exception {
    ${type} ${entity.variable()} = create();
    ${type} dto = ${name}Samples.otherDto(${tests.requiredIdArguments()}).build();

    HttpResponse<String> updated = service.send("PUT", "${path}/" + ${entity.variable()}.${entity.idField()}(), dto);

    assertEquals(200, updated.statusCode());
    ${type} body = TestService.read(updated, ${type}.class);
    assertEquals(${entity.variable()}.${entity.idField()}(), body.${entity.idField()}());
    assertEquals(dto, writable(body));
  }

  @Test
  void lists${tests.pluralType()}() throws Exception {
    ${type} ${entity.variable()} = create();

    HttpResponse<String> list = service.send("GET", "${path}");

    assertEquals(200, list.statusCode());
    JsonNode body = Json.MAPPER.readTree(list.body());
    @if(entity.paginated())
    assertEquals(0, body.path("page").path("number").asInt());
    assertEquals(20, body.path("page").path("size").asInt());
    @else
    assertTrue(body.isArray());
    @endif
    assertTrue(body.findValuesAsString("${entity.idField()}").contains(${entity.variable()}.${entity.idField()}().toString()));
  }

  @Test
  void deletes${entity.article()}${name}() throws Exception {
    ${type} ${entity.variable()} = create();

    HttpResponse<String> deleted = service.send("DELETE", "${path}/" + ${entity.variable()}.${entity.idField()}());
    HttpResponse<String> found = service.send("GET", "${path}/" + ${entity.variable()}.${entity.idField()}());

    assertEquals(204, deleted.statusCode());
    assertEquals(404, found.statusCode());
  }
  @if(tests.requiresValues())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${name}WithoutItsRequiredValues() throws Exception {
    ${type} empty = ${name}Samples.emptyDto().build();

    HttpResponse<String> created = service.send("POST", "${path}", empty);

    assertEquals(400, created.statusCode());
  }
  @endif
  @if(tests.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${name}WhoseUniqueValuesAreTaken() throws Exception {
    create();

    HttpResponse<String> created = service.send("POST", "${path}", dto());

    assertEquals(409, created.statusCode());
    assertEquals(
        "The request breaks a constraint of the database.",
        Json.MAPPER.readTree(created.body()).path("detail").asString());
  }
  @endif<%-- keeps the blank line below --%>

  /** ${entity.article()} ${entity.variable()} created through the API, with the first sample values. */
  private ${type} create() throws Exception {
    return TestService.read(service.send("POST", "${path}", dto()), ${type}.class);
  }

  /** ${entity.article()} ${entity.variable()} as the API takes it, with the first sample values. */
  private ${type} dto() {
    return ${name}Samples.dto(${tests.requiredIdArguments()}).build();
  }

  /** The DTO without what the service gives it: its identifier@if(entity.audited()) and its audit times@endif. */
  private static ${type} writable(${type} dto) {
    return new ${type}(${dto.writableArguments("dto")});
  }
}
