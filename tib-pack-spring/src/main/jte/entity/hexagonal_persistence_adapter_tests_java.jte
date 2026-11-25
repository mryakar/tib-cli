@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@import me.yakar.tib.pack.spring.JpaModel
@param EntityTestsModel tests
@param JpaModel jpa
!{EntityModel entity = tests.entity();}
!{String name = entity.name();}
!{String plural = entity.plural();}
package ${entity.persistencePackage()};

import static org.assertj.core.api.Assertions.assertThat;
@if(tests.unique())
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@endif

import ${entity.basePackage()}.TestcontainersConfiguration;
import ${entity.packageName()}.${name}Samples;
import ${entity.domainPackage()}.${name};
import ${entity.storePackage()}.${name}Store;
@for(var target : tests.requiredTargets())
import ${target.packageName()}.${target.name()}Samples;
import ${target.domainPackage()}.${target.name()};
import ${target.storePackage()}.${target.name()}Store;
import ${target.persistencePackage()}.${target.name()}PersistenceAdapter;
import ${target.persistencePackage()}.${target.name()}PersistenceMapper;
@endfor
@if(!tests.requiredTargets().isEmpty())
import org.junit.jupiter.api.BeforeEach;
@endif
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
@if(tests.unique())
import org.springframework.dao.DataIntegrityViolationException;
@endif
@if(entity.paginated())
import org.springframework.data.domain.Pageable;
@endif

/** The store of ${plural} on their table, through its adapter, against PostgreSQL in a container. */
@DataJpaTest
@Import({${jpa.persistenceClasses(tests.requiredTargets())}})
class ${name}PersistenceAdapterTests {

  @Autowired private ${name}Store ${plural};
  @for(var target : tests.requiredTargets())
  @Autowired private ${target.name()}Store ${target.plural()};
  @endfor
  @Autowired private TestEntityManager entityManager;
  @template.entity.required_targets(tests = tests, jpa = jpa)

  @Test
  void savesAndFinds${entity.article()}${name}() {
    ${name} ${entity.variable()} = ${plural}.save(${tests.sample(entity)});
    entityManager.clear();

    ${name} found = ${plural}.findById(${entity.variable()}.getId()).orElseThrow();
    @for(var field : entity.fields())
    assertThat(found.get${field.property()}()).isEqualTo(${entity.variable()}.get${field.property()}());
    @endfor
    @for(var relation : entity.requiredRelations())
    assertThat(found.get${relation.idProperty()}()).isEqualTo(${relation.target().variable()}.getId());
    @endfor
    @if(entity.audited())
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    @endif
  }

  @Test
  void lists${tests.pluralType()}() {
    ${name} ${entity.variable()} = ${plural}.save(${tests.sample(entity)});

    @if(entity.paginated())
    assertThat(${plural}.findAll(Pageable.unpaged())).extracting(${name}::getId).contains(${entity.variable()}.getId());
    @else
    assertThat(${plural}.findAll()).extracting(${name}::getId).contains(${entity.variable()}.getId());
    @endif
  }

  @Test
  void deletes${entity.article()}${name}() {
    ${name} ${entity.variable()} = ${plural}.save(${tests.sample(entity)});

    ${plural}.delete(${entity.variable()});
    entityManager.flush();
    entityManager.clear();

    assertThat(${plural}.findById(${entity.variable()}.getId())).isEmpty();
  }
  @if(tests.unique())<%-- keeps the blank line below --%>

  @Test
  void refuses${entity.article()}${name}WhoseUniqueValuesAreTaken() {
    ${plural}.save(${tests.sample(entity)});

    assertThatThrownBy(() -> ${plural}.save(${tests.sample(entity)}))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
  @endif
}
