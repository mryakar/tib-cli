@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
package ${entity.packageName()};

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ${entity.basePackage()}.NotFoundException;
import ${entity.basePackage()}.Transactions;
import java.sql.SQLException;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif
@for(var relation : entity.relations())
@if(relation.target().uuidId())
import java.util.UUID;
@endif
@endfor
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of ${entity.plural()}, with the repository and the mapper mocked. */
@ExtendWith(MockitoExtension.class)
class ${name}ServiceTests {

  private static final ${entity.idType()} ID = ${tests.sampleId()};

  @Mock private Transactions transactions;
  @Mock private ${name}Repository repository;
  @Mock private ${name}Mapper mapper;
  @InjectMocks private ${name}Service service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any())).thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void gets${entity.article()}${name}() throws SQLException {
    ${name} ${entity.variable()} = new ${name}();
    ${name}Dto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertSame(dto, service.get(ID));
  }

  @Test
  void anUnknown${name}IsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.get(ID));
  }

  @Test
  void creates${entity.article()}${name}FromItsDto() throws SQLException {
    ${name}Dto dto = dto();
    when(repository.insert(any(${name}.class))).thenAnswer(call -> call.getArgument(0));
    when(mapper.toDto(any(${name}.class))).thenReturn(dto);

    assertSame(dto, service.create(dto));
    verify(mapper).update(eq(dto), any(${name}.class));
  }

  @Test
  void updates${entity.article()}${name}FromItsDto() throws SQLException {
    ${name} ${entity.variable()} = new ${name}();
    ${name}Dto dto = dto();
    when(repository.findById(ID)).thenReturn(Optional.of(${entity.variable()}));
    when(mapper.toDto(${entity.variable()})).thenReturn(dto);

    assertSame(dto, service.update(ID, dto));
    verify(mapper).update(dto, ${entity.variable()});
    verify(repository).update(${entity.variable()});
  }

  @Test
  void deletes${entity.article()}${name}() throws SQLException {
    ${name} ${entity.variable()} = new ${name}();
    ${entity.variable()}.setId(ID);
    when(repository.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    service.delete(ID);

    verify(repository).delete(ID);
  }

  private static ${name}Dto dto() {
    return ${name}Samples.dto(${tests.sampleIdArguments()}).${entity.idField()}(ID).build();
  }
}
