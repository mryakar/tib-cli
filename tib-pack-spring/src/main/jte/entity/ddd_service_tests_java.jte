@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
package ${entity.applicationPackage()};

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ${entity.basePackage()}.NotFoundException;
import ${entity.domainPackage()}.${name};
import ${entity.domainPackage()}.${name}Repository;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of ${entity.plural()}, with the repository mocked. */
@ExtendWith(MockitoExtension.class)
class ${name}ServiceTests {

  private static final ${entity.idType()} ID = ${tests.sampleId()};

  @Mock private ${name}Repository ${entity.plural()};
  @InjectMocks private ${name}Service service;

  @Test
  void gets${entity.article()}${name}() {
    ${name} ${entity.variable()} = new ${name}();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    assertThat(service.get(ID)).isSameAs(${entity.variable()});
  }

  @Test
  void anUnknown${name}IsNotFound() {
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("There is no ${entity.variable()} " + ID);
  }

  @Test
  void creates${entity.article()}${name}() {
    ${name} ${entity.variable()} = new ${name}();
    when(${entity.plural()}.saveAndFlush(${entity.variable()})).thenReturn(${entity.variable()});

    assertThat(service.create(${entity.variable()})).isSameAs(${entity.variable()});
  }

  @Test
  void updates${entity.article()}${name}AsTheChangeDoes() {
    ${name} ${entity.variable()} = new ${name}();
    AtomicReference<${name}> changed = new AtomicReference<>();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(${entity.plural()}.saveAndFlush(${entity.variable()})).thenReturn(${entity.variable()});

    assertThat(service.update(ID, changed::set)).isSameAs(${entity.variable()});
    assertThat(changed.get()).isSameAs(${entity.variable()});
  }

  @Test
  void deletes${entity.article()}${name}() {
    ${name} ${entity.variable()} = new ${name}();
    when(${entity.plural()}.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    service.delete(ID);

    verify(${entity.plural()}).delete(${entity.variable()});
  }
}
