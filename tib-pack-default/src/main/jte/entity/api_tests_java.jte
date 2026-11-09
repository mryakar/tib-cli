@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String path = entity.path();}
!{String dto = entity.name() + "Dto";}
package ${entity.packageName()};

${tests.apiTestsImports()}/** The API of ${entity.plural()}, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ${entity.name()}ApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private ${entity.name()}Repository ${entity.plural()};
  @for(var target : tests.requiredTargets())
  @Autowired private ${target.name()}Repository ${target.plural()};
  @endfor
  @if(!tests.requiredTargets().isEmpty())<%-- keeps the blank line below --%>

  @for(var target : tests.requiredTargets())
  private ${target.name()} ${target.variable()};
  @endfor<%-- keeps the blank line below --%>

  @BeforeEach
  void saveWhatA${entity.name()}Needs() {
    @for(var target : tests.requiredTargets())
    ${target.variable()} = ${target.plural()}.saveAndFlush(new${target.name()}());
    @endfor
  }
  @endif<%-- keeps the blank line below --%>

  @AfterEach
  void deleteAll() {
    ${entity.plural()}.deleteAll();
    @for(var target : tests.requiredTargets().reversed())
    ${target.plural()}.deleteAll();
    @endfor
  }

  @Test
  void creates${tests.article()}${entity.name()}() {
    ${dto} dto = dto();

    ResponseEntity<${dto}> created = http.postForEntity("${path}", dto, ${dto}.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().${entity.idField()}()).isNotNull();
    assertThat(created.getBody())
        .usingRecursiveComparison()
        .ignoringFields(${tests.ignoredFields(true)})
        .isEqualTo(dto);
  }

  @Test
  void gets${tests.article()}${entity.name()}() {
    ${dto} ${entity.variable()} = create();

    ResponseEntity<${dto}> found =
        http.getForEntity("${path}/{id}", ${dto}.class, ${entity.variable()}.${entity.idField()}());

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    @if(entity.audited())
    assertThat(found.getBody())
        .usingRecursiveComparison()
        .ignoringFields(${tests.ignoredFields(false)})
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
  void updates${tests.article()}${entity.name()}() {
    ${dto} ${entity.variable()} = create();
    ${dto} dto = otherDto();

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
        .ignoringFields(${tests.ignoredFields(true)})
        .isEqualTo(dto);
  }

  @Test
  void lists${tests.pluralType()}() {
    ${dto} ${entity.variable()} = create();

    ResponseEntity<JsonNode> list = http.getForEntity("${path}", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsText("${entity.idField()}")).contains(${entity.variable()}.${entity.idField()}().toString());
  }

  @Test
  void deletes${tests.article()}${entity.name()}() {
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
  void refuses${tests.article()}${entity.name()}WithoutItsRequiredValues() {
    ${dto} empty = new ${dto}(${String.join(", ", tests.emptyDtoArguments())});

    ResponseEntity<String> created = http.postForEntity("${path}", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
  @endif
  @if(tests.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${tests.article()}${entity.name()}WhoseUniqueValuesAreTaken() {
    create();

    ResponseEntity<JsonNode> created = http.postForEntity("${path}", dto(), JsonNode.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(created.getBody().path("detail").asText())
        .isEqualTo("The request breaks a constraint of the database.");
  }
  @endif<%-- keeps the blank line below --%>

  /** ${tests.article()} ${entity.variable()} created through the API, with the first sample values. */
  private ${dto} create() {
    return http.postForObject("${path}", dto(), ${dto}.class);
  }

  /** ${tests.article()} ${entity.variable()} as the API takes it, with the first sample values. */
  private ${dto} dto() {
    return new ${dto}(
        @for(var argument : ForSupport.of(tests.dtoSamples(1)))
        ${argument.get()}${argument.isLast() ? ");" : ","}
        @endfor
  }

  /** ${tests.article()} ${entity.variable()} as the API takes it, with the second sample values. */
  private ${dto} otherDto() {
    return new ${dto}(
        @for(var argument : ForSupport.of(tests.dtoSamples(2)))
        ${argument.get()}${argument.isLast() ? ");" : ","}
        @endfor
  }
  @for(var model : tests.requiredTargets())<%-- keeps the blank line below --%>

  private ${model.name()} new${model.name()}() {
    ${model.name()} ${model.variable()} = new ${model.name()}();
    @for(var field : model.fields())
    ${model.variable()}.set${field.property()}(${field.sample(1)});
    @endfor
    @for(var relation : model.requiredRelations())
    ${model.variable()}.set${relation.property()}(${relation.target().variable()});
    @endfor
    return ${model.variable()};
  }
  @endfor
}
