@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
package ${entity.applicationPackage()};

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ${entity.basePackage()}.NotFoundException;
import ${entity.basePackage()}.Transactions;
import ${entity.domainPackage()}.${name};
import ${entity.domainPackage()}.${name}Repository;
import java.sql.SQLException;
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The use cases of ${entity.plural()}, with the repository mocked. */
@ExtendWith(MockitoExtension.class)
class ${name}ServiceTests {

  private static final ${entity.idType()} ID = ${tests.sampleId()};

  @Mock private Transactions transactions;
  @Mock private ${name}Repository repository;
  @InjectMocks private ${name}Service service;

  /** The transactions just run the work, without a database. */
  @BeforeEach
  void runTheWork() {
    when(transactions.run(any())).thenAnswer(call -> call.<Transactions.Work<?>>getArgument(0).run());
  }

  @Test
  void gets${entity.article()}${name}() throws SQLException {
    ${name} ${entity.variable()} = new ${name}();
    when(repository.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    assertSame(${entity.variable()}, service.get(ID));
  }

  @Test
  void anUnknown${name}IsNotFound() throws SQLException {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException e = assertThrows(NotFoundException.class, () -> service.get(ID));
    assertEquals("There is no ${entity.variable()} " + ID, e.getMessage());
  }

  @Test
  void creates${entity.article()}${name}() throws SQLException {
    ${name} ${entity.variable()} = new ${name}();
    when(repository.insert(${entity.variable()})).thenReturn(${entity.variable()});

    assertSame(${entity.variable()}, service.create(${entity.variable()}));
  }

  @Test
  void updates${entity.article()}${name}AsTheChangeDoes() throws SQLException {
    ${name} ${entity.variable()} = new ${name}();
    AtomicReference<${name}> changed = new AtomicReference<>();
    when(repository.findById(ID)).thenReturn(Optional.of(${entity.variable()}));

    assertSame(${entity.variable()}, service.update(ID, changed::set));
    assertSame(${entity.variable()}, changed.get());
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
}
