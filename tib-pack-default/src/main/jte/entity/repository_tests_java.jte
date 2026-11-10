@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
package ${entity.packageName()};

${tests.repositoryTestsImports()}/** The table of ${entity.plural()}, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ${entity.name()}RepositoryTests {

  @Autowired private ${entity.name()}Repository ${entity.plural()};
  @for(var target : tests.requiredTargets())
  @Autowired private ${target.name()}Repository ${target.plural()};
  @endfor
  @Autowired private TestEntityManager entityManager;
  @if(!tests.requiredTargets().isEmpty())<%-- keeps the blank line below --%>

  @for(var target : tests.requiredTargets())
  private ${target.name()} ${target.variable()};
  @endfor<%-- keeps the blank line below --%>

  @BeforeEach
  void saveWhat${entity.article()}${entity.name()}Needs() {
    @for(var target : tests.requiredTargets())
    ${target.variable()} = ${target.plural()}.saveAndFlush(${target.name()}Samples.${target.variable()}(${EntityTestsModel.requiredArguments(target)}).build());
    @endfor
  }
  @endif<%-- keeps the blank line below --%>

  @Test
  void savesAndFinds${entity.article()}${entity.name()}() {
    ${entity.name()} ${entity.variable()} = ${entity.plural()}.saveAndFlush(${entity.name()}Samples.${entity.variable()}(${EntityTestsModel.requiredArguments(entity)}).build());
    entityManager.clear();

    ${entity.name()} found = ${entity.plural()}.findById(${entity.variable()}.getId()).orElseThrow();
    @for(var field : entity.fields())
    assertThat(found.get${field.property()}()).isEqualTo(${entity.variable()}.get${field.property()}());
    @endfor
    @for(var relation : entity.requiredRelations())
    assertThat(found.get${relation.property()}().getId()).isEqualTo(${relation.target().variable()}.getId());
    @endfor
    @if(entity.audited())
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    @endif
  }
  @if(tests.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${entity.name()}WhoseUniqueValuesAreTaken() {
    ${entity.plural()}.saveAndFlush(${entity.name()}Samples.${entity.variable()}(${EntityTestsModel.requiredArguments(entity)}).build());

    assertThatThrownBy(() -> ${entity.plural()}.saveAndFlush(${entity.name()}Samples.${entity.variable()}(${EntityTestsModel.requiredArguments(entity)}).build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
  @endif
}
