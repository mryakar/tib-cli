@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
package ${entity.packageName()};

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif
@for(var relation : entity.relations())
@if(relation.target().uuidId())
import java.util.UUID;
@endif
@endfor
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of ${entity.plural()}, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class ${name}ServiceTests {

  private static final ${entity.idType()} ID = ${tests.sampleId()};

  @Mock private ${name}Repository ${entity.plural()};
  @Mock private ${name}Mapper mapper;
  @InjectMocks private ${name}Service service;

  @Test
  void gets${entity.article()}${name}() {
    ${name} ${entity.variable()} = new ${name}();
    ${name}Dto dto = dto();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertThat(service.get(ID)).isSameAs(dto);
  }

  @Test
  void anUnknown${name}IsNotFound() {
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void creates${entity.article()}${name}FromItsDto() {
    ${name}Dto dto = dto();
    when(${entity.plural()}.saveAndFlush(any(${name}.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(${name}.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(${name}.class));
  }

  @Test
  void updates${entity.article()}${name}FromItsDto() {
    ${name} ${entity.variable()} = new ${name}();
    ${name}Dto dto = dto();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(${entity.plural()}.saveAndFlush(${entity.variable()})).thenReturn(${entity.variable()});
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, ${entity.variable()});
  }

  @Test
  void deletes${entity.article()}${name}() {
    ${name} ${entity.variable()} = new ${name}();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    service.delete(ID);

    verify(${entity.plural()}).delete(${entity.variable()});
  }

  private static ${name}Dto dto() {
    return ${name}Samples.dto(${tests.sampleIdArguments()}).${entity.idField()}(ID).build();
  }
}
