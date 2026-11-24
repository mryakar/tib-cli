@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String repository = entity.variable() + "Repository";}
package ${entity.persistencePackage()};

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
@if(tests.unique())
import static org.junit.jupiter.api.Assertions.assertThrows;
@endif

import ${entity.basePackage()}.TestDatabase;
import ${entity.basePackage()}.Transactions;
${EntityModel.importOf(entity.persistencePackage(), entity.packageName(), name + "Samples")}
@for(var target : tests.requiredTargets())
${EntityModel.importOf(entity.persistencePackage(), target.domainPackage(), target.name())}
${EntityModel.importOf(entity.persistencePackage(), target.persistencePackage(), target.name() + "Repository")}
${EntityModel.importOf(entity.persistencePackage(), target.packageName(), target.name() + "Samples")}
@endfor
import org.junit.jupiter.api.AfterEach;
@if(!tests.requiredTargets().isEmpty())
import org.junit.jupiter.api.BeforeEach;
@endif
import org.junit.jupiter.api.Test;

/** The table of ${entity.plural()}, against PostgreSQL in a container. */
class ${name}RepositoryTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final ${name}Repository ${repository} = new ${name}Repository(transactions);
  @for(var target : tests.requiredTargets())
  private final ${target.name()}Repository ${target.variable()}Repository = new ${target.name()}Repository(transactions);
  @endfor
  @template.entity.required_targets(tests = tests)

  @AfterEach
  void deleteAll() {
    transactions.run(
        () -> {
          ${repository}.deleteAll();
          @for(var target : tests.requiredTargets().reversed())
          ${target.variable()}Repository.deleteAll();
          @endfor
          return null;
        });
  }

  @Test
  void savesAndFinds${entity.article()}${name}() {
    ${name} ${entity.variable()} = transactions.run(() -> ${repository}.insert(${tests.sample(entity)}));

    ${name} found = transactions.run(() -> ${repository}.findById(${entity.variable()}.getId())).orElseThrow();
    @for(var field : entity.fields())
    assertEquals(${entity.variable()}.get${field.property()}(), found.get${field.property()}());
    @endfor
    @for(var relation : entity.requiredRelations())
    assertEquals(${relation.target().variable()}.getId(), found.get${relation.idProperty()}());
    @endfor
    @if(entity.audited())
    assertNotNull(found.getCreatedAt());
    assertEquals(${entity.variable()}.getUpdatedAt(), found.getUpdatedAt());
    @endif
  }

  @Test
  void updates${entity.article()}${name}() {
    ${name} ${entity.variable()} = transactions.run(() -> ${repository}.insert(${tests.sample(entity)}));
    ${name} changed = ${name}Samples.other${name}(${tests.requiredIdArguments()}).build();
    changed.setId(${entity.variable()}.getId());

    assertTrue(transactions.run(() -> ${repository}.update(changed)));
    ${name} found = transactions.run(() -> ${repository}.findById(${entity.variable()}.getId())).orElseThrow();
    @for(var field : entity.fields())
    assertEquals(changed.get${field.property()}(), found.get${field.property()}());
    @endfor
    @if(entity.audited())
    assertEquals(changed.getUpdatedAt(), found.getUpdatedAt());
    @endif
  }

  @Test
  void lists${tests.pluralType()}() {
    ${name} ${entity.variable()} = transactions.run(() -> ${repository}.insert(${tests.sample(entity)}));

    @if(entity.paginated())
    assertEquals(
        ${entity.variable()}.getId(),
        transactions.run(() -> ${repository}.findAll(0, 20)).content().getFirst().getId());
    @else
    assertEquals(
        ${entity.variable()}.getId(), transactions.run(() -> ${repository}.findAll()).getFirst().getId());
    @endif
    assertEquals(1, transactions.run(${repository}::count));
  }

  @Test
  void deletes${entity.article()}${name}() {
    ${name} ${entity.variable()} = transactions.run(() -> ${repository}.insert(${tests.sample(entity)}));

    assertTrue(transactions.run(() -> ${repository}.delete(${entity.variable()}.getId())));
    assertFalse(transactions.run(() -> ${repository}.findById(${entity.variable()}.getId())).isPresent());
  }
  @if(tests.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${name}WhoseUniqueValuesAreTaken() {
    transactions.run(() -> ${repository}.insert(${tests.sample(entity)}));

    Transactions.DatabaseException e =
        assertThrows(
            Transactions.DatabaseException.class,
            () -> transactions.run(() -> ${repository}.insert(${tests.sample(entity)})));
    assertTrue(e.getCause().getSQLState().startsWith("23"));
  }
  @endif
}
