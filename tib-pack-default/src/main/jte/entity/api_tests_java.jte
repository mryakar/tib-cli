@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String path = "/api/" + entity.plural();}
!{String dto = entity.name() + "Dto";}
// Written by tib when the entity was created; it is yours from then on.
package ${entity.packageName()};

${entity.apiTestsImports()}/** The API of ${entity.plural()}, over HTTP, against PostgreSQL in a container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ${entity.name()}ApiTests {

  @Autowired private TestRestTemplate http;
  @Autowired private ${entity.name()}Repository ${entity.plural()};
  @for(var target : entity.requiredTargets())
  @Autowired private ${target.name()}Repository ${target.plural()};
  @endfor
  @if(!entity.requiredTargets().isEmpty())<%-- keeps the blank line below --%>

  @for(var target : entity.requiredTargets())
  private ${target.name()} ${target.variable()};
  @endfor<%-- keeps the blank line below --%>

  @BeforeEach
  void saveWhatA${entity.name()}Needs() {
    @for(var target : entity.requiredTargets())
    ${target.variable()} = ${target.plural()}.saveAndFlush(new${target.name()}());
    @endfor
  }
  @endif<%-- keeps the blank line below --%>

  @AfterEach
  void deleteAll() {
    ${entity.plural()}.deleteAll();
    @for(var target : entity.reversedRequiredTargets())
    ${target.plural()}.deleteAll();
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
        .ignoringFields(${entity.ignoredFields(true)})
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
        .ignoringFields(${entity.ignoredFields(false)})
        .isEqualTo(${entity.variable()});
    @else
    assertThat(found.getBody()).isEqualTo(${entity.variable()});
    @endif
  }

  @Test
  void anUnknown${entity.name()}IsNotFound() {
    ResponseEntity<String> found =
        http.getForEntity("${path}/{id}", String.class, ${entity.unknownId()});

    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updates${entity.article()}${entity.name()}() {
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
        .ignoringFields(${entity.ignoredFields(true)})
        .isEqualTo(dto);
  }

  @Test
  void lists${entity.pluralType()}() {
    ${dto} ${entity.variable()} = create();

    ResponseEntity<JsonNode> list = http.getForEntity("${path}", JsonNode.class);

    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody().findValuesAsText("${entity.idField()}")).contains(${entity.variable()}.${entity.idField()}().toString());
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
  @if(entity.requiresValues())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${entity.name()}WithoutItsRequiredValues() {
    ${dto} empty = new ${dto}(${String.join(", ", entity.emptyDtoArguments())});

    ResponseEntity<String> created = http.postForEntity("${path}", empty, String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
  @endif<%-- keeps the blank line below --%>

  /** ${entity.article()} ${entity.variable()} created through the API, with the first sample values. */
  private ${dto} create() {
    return http.postForObject("${path}", dto(), ${dto}.class);
  }

  /** ${entity.article()} ${entity.variable()} as the API takes it, with the first sample values. */
  private ${dto} dto() {
    return new ${dto}(
        @for(var argument : ForSupport.of(entity.dtoSamples(1)))
        ${argument.get()}${argument.isLast() ? ");" : ","}
        @endfor
  }

  /** ${entity.article()} ${entity.variable()} as the API takes it, with the second sample values. */
  private ${dto} otherDto() {
    return new ${dto}(
        @for(var argument : ForSupport.of(entity.dtoSamples(2)))
        ${argument.get()}${argument.isLast() ? ");" : ","}
        @endfor
  }
  @for(var model : entity.requiredTargets())<%-- keeps the blank line below --%>

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
