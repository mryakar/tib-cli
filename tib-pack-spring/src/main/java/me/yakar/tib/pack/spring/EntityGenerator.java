/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.yakar.tib.pack.spring;

import java.util.Map;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.EntityTestsModel;
import me.yakar.tib.pack.Migration;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates the entity layer of every entity of the spec, with its tests, each entity after the
 * entities it refers to, so that its migration can reference their tables.
 */
final class EntityGenerator implements Generator {

  /** The header of a file of tib's. */
  private static final String EVERY_RUN =
      "Written by tib on every run. Do not edit: your changes would be lost.";

  /** The header of a file of the developer's. */
  private static final String ONCE =
      "Written by tib when the entity was created; it is yours from then on.";

  /** The header of a file of the developer's that the service needs once it has entities. */
  private static final String FIRST_ENTITY =
      "Written by tib with the first entity of the service; it is yours from then on.";

  @Override
  public void generate(Project project, ProjectFiles files) {
    Map<String, EntityModel> models = EntityModel.models(project, false);
    models.values().forEach(model -> generateEntity(project, model, files));
    // Written here and not by init: only a service with an API needs it.
    if (models.values().stream().anyMatch(EntityModel::layer)) {
      firstEntity(files, project, "ConflictHandler", "entity/conflict_handler_java.jte");
      // The services of DDD and hexagonal throw it, and the handler answers it: they know no HTTP.
      if (project.architecture() != Architecture.ANEMIC) {
        firstEntity(files, project, "NotFoundException", "entity/not_found_exception_java.jte");
      }
    }
  }

  /** Writes a class of the base package once, with the first entity that needs it. */
  private static void firstEntity(
      ProjectFiles files, Project project, String type, String template) {
    String path = EntityModel.directory(project.basePackage()) + "/" + type + ".java";
    files.once(
        path,
        SpringPack.TEMPLATES.render(path, FIRST_ENTITY, template, Map.of("project", project)));
  }

  private static void generateEntity(Project project, EntityModel entity, ProjectFiles files) {
    String name = "/" + entity.name();
    String domain = EntityModel.directory(entity.domainPackage()) + name;
    String application = EntityModel.directory(entity.applicationPackage()) + name;
    String web = EntityModel.directory(entity.webPackage()) + name;
    String persistence = EntityModel.directory(entity.persistencePackage()) + name;
    // DDD and hexagonal have services of their own: the use cases of the domain, which know no DTO.
    String architecture = architecture(project);
    Map<String, Object> model = Map.of("entity", entity);
    Map<String, Object> tests = Map.of("tests", new EntityTestsModel(entity));
    if (entity.hexagonal()) {
      // The domain knows no JPA: the table is mapped by a class of the persistence adapter, which
      // keeps the domain through the store, the port of the application.
      own(files, domain + "Generated.java", "entity/hexagonal_domain_generated_java.jte", model);
      once(files, domain + ".java", "entity/hexagonal_domain_java.jte", model);
      own(files, persistence + "Entity.java", "entity/generated_java.jte", model);
      own(
          files,
          persistence + "PersistenceMapper.java",
          "entity/hexagonal_persistence_mapper_java.jte",
          model);
      once(
          files,
          EntityModel.directory(entity.storePackage()) + name + "Store.java",
          "entity/hexagonal_store_java.jte",
          model);
      once(
          files,
          persistence + "PersistenceAdapter.java",
          "entity/hexagonal_persistence_adapter_java.jte",
          model);
    } else {
      own(files, domain + "Generated.java", "entity/generated_java.jte", model);
      once(files, domain + ".java", "entity/entity_java.jte", model);
    }
    once(
        files,
        EntityModel.directory(entity.persistencePackage()) + "/" + entity.repository() + ".java",
        "entity/repository_java.jte",
        model);
    if (entity.dto()) {
      own(files, web + "Dto.java", "entity/dto_java.jte", model);
    }
    // A migration lasts: a database may have run it, so it stays when the entity leaves the spec.
    Migration.write(files, entity);
    // The tests of the kinds of the spec; the service and the API only of an entity with a layer.
    boolean repositoryTests = project.tests(TestKind.INTEGRATION);
    boolean serviceTests = entity.layer() && project.tests(TestKind.UNIT);
    boolean apiTests = entity.layer() && project.tests(TestKind.API);
    if (repositoryTests || serviceTests || apiTests) {
      own(
          files,
          EntityModel.testDirectory(entity.packageName()) + name + "Samples.java",
          "entity/samples_java.jte",
          tests);
    }
    if (repositoryTests && entity.hexagonal()) {
      once(
          files,
          EntityModel.testDirectory(entity.persistencePackage())
              + name
              + "PersistenceAdapterTests.java",
          "entity/hexagonal_persistence_adapter_tests_java.jte",
          tests);
    } else if (repositoryTests) {
      once(
          files,
          EntityModel.testDirectory(entity.domainPackage()) + name + "RepositoryTests.java",
          "entity/repository_tests_java.jte",
          tests);
    }
    if (entity.layer()) {
      own(
          files,
          EntityModel.directory(entity.webPackage()) + "/" + entity.webMapper() + ".java",
          "entity/mapper_java.jte",
          model);
      if (entity.hexagonal()) {
        once(
            files,
            EntityModel.directory(entity.useCasePackage()) + name + "UseCase.java",
            "entity/hexagonal_use_case_java.jte",
            model);
      }
      once(
          files,
          application + "Service.java",
          "entity/" + architecture + "service_java.jte",
          model);
      once(files, web + "Controller.java", "entity/" + architecture + "controller_java.jte", model);
    }
    if (serviceTests) {
      once(
          files,
          EntityModel.testDirectory(entity.applicationPackage()) + name + "ServiceTests.java",
          "entity/" + architecture + "service_tests_java.jte",
          tests);
    }
    if (apiTests) {
      once(
          files,
          EntityModel.testDirectory(entity.webPackage()) + name + "ApiTests.java",
          "entity/api_tests_java.jte",
          tests);
    }
  }

  /**
   * The prefix of the templates of the service, its tests and the controller in the architecture of
   * the project: none for the anemic one, whose service maps the entity to and from its DTO.
   */
  private static String architecture(Project project) {
    return switch (project.architecture()) {
      case ANEMIC -> "";
      case DDD -> "ddd_";
      case HEXAGONAL -> "hexagonal_";
    };
  }

  /** Writes a file of tib's: on every run. */
  private static void own(
      ProjectFiles files, String path, String template, Map<String, Object> parameters) {
    files.own(path, SpringPack.TEMPLATES.render(path, EVERY_RUN, template, parameters));
  }

  /** Writes a file of the developer's: once. */
  private static void once(
      ProjectFiles files, String path, String template, Map<String, Object> parameters) {
    files.once(path, SpringPack.TEMPLATES.render(path, ONCE, template, parameters));
  }
}
