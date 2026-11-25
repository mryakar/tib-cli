@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@import me.yakar.tib.pack.Sources
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String repository = entity.variable() + "Repository";}
package ${entity.persistencePackage()};

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@if(tests.unique())
import static org.junit.jupiter.api.Assertions.assertThrows;
@endif
import static org.junit.jupiter.api.Assertions.assertTrue;

import ${entity.basePackage()}.TestDatabase;
import ${entity.basePackage()}.Transactions;
${Sources.importOf(entity.persistencePackage(), entity.domainPackage(), name)}
${Sources.importOf(entity.persistencePackage(), entity.packageName(), name + "Samples")}
@for(var target : tests.requiredTargets())
${Sources.importOf(entity.persistencePackage(), target.domainPackage(), target.name())}
${Sources.importOf(entity.persistencePackage(), target.persistencePackage(), target.name() + "Repository")}
${Sources.importOf(entity.persistencePackage(), target.packageName(), target.name() + "Samples")}
@endfor
import org.junit.jupiter.api.AfterEach;
@if(!tests.requiredTargets().isEmpty())
import org.junit.jupiter.api.BeforeEach;
@endif
import org.junit.jupiter.api.Test;

/** The store of ${entity.plural()} on their table, against PostgreSQL in a container. */
class ${name}PersistenceAdapterTests {

  private final Transactions transactions = new Transactions(TestDatabase.dataSource());
  private final ${name}Repository ${repository} = new ${name}Repository(transactions);
  private final ${name}PersistenceAdapter store = new ${name}PersistenceAdapter(transactions, ${repository});
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
    ${name} ${entity.variable()} = store.insert(${tests.sample(entity)});

    ${name} found = store.findById(${entity.variable()}.getId()).orElseThrow();
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
    ${name} ${entity.variable()} = store.insert(${tests.sample(entity)});
    ${name} changed = ${name}Samples.other${name}(${tests.requiredIdArguments()}).build();
    changed.setId(${entity.variable()}.getId());

    assertTrue(store.update(changed));
    ${name} found = store.findById(${entity.variable()}.getId()).orElseThrow();
    @for(var field : entity.fields())
    assertEquals(changed.get${field.property()}(), found.get${field.property()}());
    @endfor
    @if(entity.audited())
    assertEquals(changed.getUpdatedAt(), found.getUpdatedAt());
    @endif
  }

  @Test
  void lists${tests.pluralType()}() {
    ${name} ${entity.variable()} = store.insert(${tests.sample(entity)});

    @if(entity.paginated())
    assertEquals(${entity.variable()}.getId(), store.findAll(0, 20).content().getFirst().getId());
    assertEquals(1, store.findAll(0, 20).page().totalElements());
    @else
    assertEquals(${entity.variable()}.getId(), store.findAll().getFirst().getId());
    assertEquals(1, store.findAll().size());
    @endif
  }

  @Test
  void deletes${entity.article()}${name}() {
    ${name} ${entity.variable()} = store.insert(${tests.sample(entity)});

    assertTrue(store.delete(${entity.variable()}.getId()));
    assertFalse(store.findById(${entity.variable()}.getId()).isPresent());
  }
  @if(tests.unique())

  @Test
  void refuses${entity.article()}${name}WhoseUniqueValuesAreTaken() {
    store.insert(${tests.sample(entity)});

    Transactions.DatabaseException e =
        assertThrows(Transactions.DatabaseException.class, () -> store.insert(${tests.sample(entity)}));
    assertTrue(e.getCause().getSQLState().startsWith("23"));
  }
  @endif
}
