@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib when the entity was created; it is yours from then on.
package ${entity.packageName()};

${entity.repositoryTestsImports()}/** The table of ${entity.plural()}, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ${entity.name()}RepositoryTests {

  @Autowired private ${entity.name()}Repository ${entity.plural()};
  @for(var target : entity.requiredTargets())
  @Autowired private ${target.name()}Repository ${target.plural()};
  @endfor
  @Autowired private TestEntityManager entityManager;
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

  @Test
  void savesAndFinds${entity.article()}${entity.name()}() {
    ${entity.name()} ${entity.variable()} = ${entity.plural()}.saveAndFlush(new${entity.name()}());
    entityManager.clear();

    ${entity.name()} found = ${entity.plural()}.findById(${entity.variable()}.getId()).orElseThrow();
    @for(var assertion : entity.foundAssertions())
    ${assertion}
    @endfor
  }
  @if(entity.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${entity.name()}WhoseUniqueValuesAreTaken() {
    ${entity.plural()}.saveAndFlush(new${entity.name()}());

    assertThatThrownBy(() -> ${entity.plural()}.saveAndFlush(new${entity.name()}()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
  @endif
  @for(var model : entity.requiredTargets())<%-- keeps the blank line below --%>

  private ${model.name()} new${model.name()}() {
    ${model.name()} ${model.variable()} = new ${model.name()}();
    @for(var setter : model.sampleSetters())
    ${setter}
    @endfor
    return ${model.variable()};
  }
  @endfor<%-- keeps the blank line below --%>

  private ${entity.name()} new${entity.name()}() {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    @for(var setter : entity.sampleSetters())
    ${setter}
    @endfor
    return ${entity.variable()};
  }
}
