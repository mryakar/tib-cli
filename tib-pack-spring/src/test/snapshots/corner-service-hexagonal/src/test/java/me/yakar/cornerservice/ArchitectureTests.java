// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * The rules of the hexagonal architecture, checked on the classes of the service by their packages:
 * the domain knows neither JPA nor Spring, the application does not use the adapters, an inbound
 * adapter does not use an outbound one, nor the reverse, and there is no cycle between the packages
 * of the entities.
 */
class ArchitectureTests {

  private static final String BASE = "me.yakar.cornerservice";

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE);

  @Test
  void theDomainKnowsNeitherJpaNorSpring() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".*.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.springframework..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void theApplicationDoesNotUseTheAdapters() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".*.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + ".*.adapter..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void theInboundAdaptersDoNotUseTheOutboundOnes() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".*.adapter.in..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + ".*.adapter.out..")
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void theOutboundAdaptersDoNotUseTheInboundOnes() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".*.adapter.out..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + ".*.adapter.in..")
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
