@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
package ${entity.packageName()};

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
@if(entity.uuid())
import java.util.UUID;
@endif
@for(var relation : entity.relations())
@if(relation.target().uuid())
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
class ${entity.name()}ServiceTests {

  private static final ${entity.idType()} ID = ${tests.sampleId()};

  @Mock private ${entity.name()}Repository ${entity.plural()};
  @Mock private ${entity.name()}Mapper mapper;
  @InjectMocks private ${entity.name()}Service service;

  @Test
  void gets${entity.article()}${entity.name()}() {
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
  void creates${entity.article()}${entity.name()}FromItsDto() {
    ${entity.name()}Dto dto = dto();
    when(${entity.plural()}.saveAndFlush(any(${entity.name()}.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(${entity.name()}.class))).thenReturn(dto);

    assertThat(service.create(dto)).isSameAs(dto);
    verify(mapper).update(eq(dto), any(${entity.name()}.class));
  }

  @Test
  void updates${entity.article()}${entity.name()}FromItsDto() {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    ${entity.name()}Dto dto = dto();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(${entity.plural()}.saveAndFlush(${entity.variable()})).thenReturn(${entity.variable()});
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertThat(service.update(ID, dto)).isSameAs(dto);
    verify(mapper).update(dto, ${entity.variable()});
  }

  @Test
  void deletes${entity.article()}${entity.name()}() {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    service.delete(ID);

    verify(${entity.plural()}).delete(${entity.variable()});
  }

  private static ${entity.name()}Dto dto() {
    return ${entity.name()}Samples.dto(${tests.sampleIdArguments()}).${entity.idField()}(ID).build();
  }
}
