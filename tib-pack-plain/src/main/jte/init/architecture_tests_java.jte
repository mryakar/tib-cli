@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * The rules of the anemic architecture, checked on the classes of the service by their names and
 * packages, as no annotation marks them: the handler of an entity over its service over its
 * repository, a handler used by the wiring alone, and no cycle between the packages
 * of the entities.
 */
class ArchitectureTests {

  private static final String BASE = "${project.basePackage()}";

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE);

  private static final DescribedPredicate<JavaClass> HANDLERS = ofEntities("Handler", "handlers");

  private static final DescribedPredicate<JavaClass> SERVICES = ofEntities("Service", "services");

  private static final DescribedPredicate<JavaClass> REPOSITORIES = ofEntities("Repository", "repositories");

  /** The entities: the classes of the generated values of an entity, and those that extend them. */
  private static final DescribedPredicate<JavaClass> ENTITIES =
      resideInAPackage(BASE + ".*..")
          .and(assignableTo(simpleNameEndingWith("Generated")))
          .as("entities");

  /** The wiring of an entity, which makes its handler, its service and its repository by hand. */
  private static final DescribedPredicate<JavaClass> WIRING = ofEntities("Wiring", "wiring");

  @Test
  void onlyHandlersAndTheWiringUseHandlers() {
    noClasses()
        .that(not(HANDLERS).and(not(WIRING)))
        .should()
        .dependOnClassesThat(HANDLERS)
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void handlersUseServicesAndNotRepositories() {
    noClasses()
        .that(HANDLERS)
        .should()
        .dependOnClassesThat(REPOSITORIES)
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void entitiesAndRepositoriesUseNoServiceAndNoHandler() {
    noClasses()
        .that(ENTITIES.or(REPOSITORIES))
        .should()
        .dependOnClassesThat(SERVICES.or(HANDLERS))
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void thePackagesOfTheEntitiesHaveNoCycle() {
    slices()
        .matching(BASE + ".(*)..")
        .should()
        .beFreeOfCycles()
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  /** The classes of a kind in the packages of the entities, by the end of their names. */
  private static DescribedPredicate<JavaClass> ofEntities(String suffix, String kind) {
    return resideInAPackage(BASE + ".*..").and(simpleNameEndingWith(suffix)).as(kind);
  }
}
