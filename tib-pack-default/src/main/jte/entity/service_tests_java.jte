@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
package ${entity.packageName()};

${tests.serviceTestsImports()}/** The use cases of ${entity.plural()}, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class ${entity.name()}ServiceTests {

  private static final ${entity.idType()} ID = ${tests.sampleId()};

  @Mock private ${entity.name()}Repository ${entity.plural()};
  @Mock private ${entity.name()}Mapper mapper;
  @InjectMocks private ${entity.name()}Service service;

  @Test
  void gets${tests.article()}${entity.name()}() {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    ${entity.name()}Dto dto = dto();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknown${entity.name()}IsNotFound() {
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void creates${tests.article()}${entity.name()}FromItsDto() {
    ${entity.name()}Dto dto = dto();
    when(${entity.plural()}.saveAndFlush(any(${entity.name()}.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(${entity.name()}.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(${entity.name()}.class));
  }

  @Test
  void updates${tests.article()}${entity.name()}FromItsDto() {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    ${entity.name()}Dto dto = dto();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(${entity.plural()}.saveAndFlush(${entity.variable()})).thenReturn(${entity.variable()});
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, ${entity.variable()});
  }

  @Test
  void deletes${tests.article()}${entity.name()}() {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    service.delete(ID);

    verify(${entity.plural()}).delete(${entity.variable()});
  }

  private static ${entity.name()}Dto dto() {
    return new ${entity.name()}Dto(
        @for(var argument : ForSupport.of(tests.serviceDtoArguments()))
        ${argument.get()}${argument.isLast() ? ");" : ","}
        @endfor
  }
}
