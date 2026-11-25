@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * The rules of the anemic architecture, checked on the classes of the service: the controller of
 * an entity over its service over its repository, and no cycle between the packages of the
 * entities.
 */
class ArchitectureTests {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("${project.basePackage()}");

  @Test
  void onlyControllersUseControllers() {
    noClasses()
        .that()
        .areNotAnnotatedWith(RestController.class)
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(RestController.class)
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void controllersUseServicesAndNotRepositories() {
    noClasses()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .dependOnClassesThat()
        .areAssignableTo(Repository.class)
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void entitiesAndRepositoriesUseNoServiceAndNoController() {
    noClasses()
        .that()
        .areAnnotatedWith(Entity.class)
        .or()
        .areAnnotatedWith(MappedSuperclass.class)
        .or()
        .areAssignableTo(Repository.class)
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(Service.class)
        .orShould()
        .dependOnClassesThat()
        .areAnnotatedWith(RestController.class)
        .allowEmptyShould(true)
        .check(CLASSES);
  }

  @Test
  void thePackagesOfTheEntitiesHaveNoCycle() {
    slices()
        .matching("${project.basePackage()}.(*)..")
        .should()
        .beFreeOfCycles()
        .allowEmptyShould(true)
        .check(CLASSES);
  }
}
