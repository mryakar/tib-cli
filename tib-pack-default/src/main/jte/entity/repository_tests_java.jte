@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
!{EntityModel entity = tests.entity();}
package ${entity.packageName()};

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ${entity.basePackage()}.TestcontainersConfiguration;
@for(var target : tests.requiredTargets())
import ${target.packageName()}.${target.name()};
import ${target.packageName()}.${target.name()}Repository;
import ${target.packageName()}.${target.name()}Samples;
@endfor
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** The table of ${entity.plural()}, against PostgreSQL in a container. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ${entity.name()}RepositoryTests {

  @Autowired private ${entity.name()}Repository ${entity.plural()};
  @for(var target : tests.requiredTargets())
  @Autowired private ${target.name()}Repository ${target.plural()};
  @endfor
  @Autowired private TestEntityManager entityManager;
  @template.entity.required_targets(tests = tests)

  @Test
  void savesAndFinds${entity.article()}${entity.name()}() {
    ${entity.name()} ${entity.variable()} = ${entity.plural()}.saveAndFlush(${tests.sample(entity)});
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
    ${entity.plural()}.saveAndFlush(${tests.sample(entity)});

    assertThatThrownBy(() -> ${entity.plural()}.saveAndFlush(${tests.sample(entity)}))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
  @endif
}
