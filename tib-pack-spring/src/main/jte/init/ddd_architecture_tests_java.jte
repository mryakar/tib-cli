@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * The rules of the DDD architecture, checked on the classes of the service: the domain of an entity
 * uses neither its application nor its web, the application does not use the web, the domain of an
 * aggregate does not use the domain of another, and there is no cycle between the packages of the
 * entities.
 */
class ArchitectureTests {

  private static final String BASE = "${project.basePackage()}";

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE);

  @Test
  void theDomainUsesNeitherTheApplicationNorTheWeb() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".*.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(BASE + ".*.application..", BASE + ".*.web..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void theApplicationDoesNotUseTheWeb() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".*.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + ".*.web..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void theDomainOfAnAggregateDoesNotUseTheDomainOfAnother() {
    slices()
        .matching(BASE + ".(*).domain..")
        .should()
        .notDependOnEachOther()
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
}
