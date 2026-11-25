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
package me.yakar.tib.pack.plain;

import java.util.Collection;
import java.util.Map;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.EntityTestsModel;
import me.yakar.tib.pack.Migration;
import me.yakar.tib.pack.Sources;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates the entity layer of every entity of the spec, with its tests, each entity after the
 * entities it refers to, so that its migration can reference their tables; and the routes of the
 * API, which wire the classes of the entities by hand. An entity refers to another by the
 * identifier of its target: there is no ORM.
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
    Map<String, EntityModel> models = EntityModel.models(project, true);
    models.values().forEach(model -> generateEntity(project, model, files));
    // Written here and not by init: only a service with an API needs them.
    if (models.values().stream().anyMatch(EntityModel::layer)) {
      firstEntity(files, project, "NotFoundException", "entity/not_found_exception_java.jte");
      firstEntity(files, project, "BadRequestException", "entity/bad_request_exception_java.jte");
      firstEntity(files, project, "ErrorHandler", "entity/error_handler_java.jte");
      firstEntity(files, project, "Http", "entity/http_java.jte");
    }
    // The repository of an entity whose list comes in pages gives one.
    if (models.values().stream().anyMatch(EntityModel::paginated)) {
      firstEntity(files, project, "Page", "entity/page_java.jte");
    }
    routes(project, models.values(), files);
  }

  /**
   * Writes the routes of the API, which tib keeps in step with the entities: the classes of every
   * entity with a service and a handler, wired by hand.
   */
  static void routes(Project project, Collection<EntityModel> entities, ProjectFiles files) {
    String path = Sources.main(project.basePackage(), "Routes");
    files.own(
        path,
        PlainPack.TEMPLATES.render(
            path,
            EVERY_RUN,
            "entity/routes_java.jte",
            Map.of(
                "project",
                project,
                "entities",
                entities.stream().filter(EntityModel::layer).toList())));
  }

  /** Writes a class of the base package once, with the first entity that needs it. */
  private static void firstEntity(
      ProjectFiles files, Project project, String type, String template) {
    String path = Sources.main(project.basePackage(), type);
    files.once(
        path, PlainPack.TEMPLATES.render(path, FIRST_ENTITY, template, Map.of("project", project)));
  }

  private static void generateEntity(Project project, EntityModel entity, ProjectFiles files) {
    String name = entity.name();
    String domain = entity.domainPackage();
    String application = entity.applicationPackage();
    String web = entity.webPackage();
    String persistence = entity.persistencePackage();
    // DDD and hexagonal have services of their own, on the domain, and handlers that map it to its
    // DTO.
    String architecture = architecture(project);
    Map<String, Object> model =
        Map.of(
            "entity",
            entity,
            "dto",
            new DtoModel(entity),
            "repository",
            new RepositoryModel(entity));
    Map<String, Object> tests =
        Map.of("tests", new EntityTestsModel(entity), "dto", new DtoModel(entity));
    own(files, Sources.main(domain, name + "Generated"), "entity/generated_java.jte", model);
    once(files, Sources.main(domain, name), "entity/entity_java.jte", model);
    // The repository is the SQL of the table that the spec makes, so it follows the spec.
    own(files, Sources.main(persistence, name + "Repository"), "entity/repository_java.jte", model);
    if (entity.hexagonal()) {
      // The application keeps the entity through the store, its port, which the persistence
      // adapter implements on the repository.
      once(
          files,
          Sources.main(entity.storePackage(), name + "Store"),
          "entity/hexagonal_store_java.jte",
          model);
      once(
          files,
          Sources.main(persistence, name + "PersistenceAdapter"),
          "entity/hexagonal_persistence_adapter_java.jte",
          model);
    }
    if (entity.dto()) {
      own(files, Sources.main(web, name + "Dto"), "entity/dto_java.jte", model);
    }
    Migration.write(files, entity);
    // The tests of the kinds of the spec; the service and the API only of an entity with a layer.
    boolean repositoryTests = project.tests(TestKind.INTEGRATION);
    boolean serviceTests = entity.layer() && project.tests(TestKind.UNIT);
    boolean apiTests = entity.layer() && project.tests(TestKind.API);
    if (repositoryTests || serviceTests || apiTests) {
      own(
          files,
          Sources.test(entity.packageName(), name + "Samples"),
          "entity/samples_java.jte",
          tests);
    }
    if (repositoryTests && entity.hexagonal()) {
      once(
          files,
          Sources.test(persistence, name + "PersistenceAdapterTests"),
          "entity/hexagonal_persistence_adapter_tests_java.jte",
          tests);
    } else if (repositoryTests) {
      once(
          files,
          Sources.test(persistence, name + "RepositoryTests"),
          "entity/repository_tests_java.jte",
          tests);
    }
    if (entity.layer()) {
      own(files, Sources.main(web, entity.webMapper()), "entity/mapper_java.jte", model);
      if (entity.hexagonal()) {
        once(
            files,
            Sources.main(entity.useCasePackage(), name + "UseCase"),
            "entity/hexagonal_use_case_java.jte",
            model);
      }
      once(
          files,
          Sources.main(application, name + "Service"),
          "entity/" + architecture + "service_java.jte",
          model);
      once(
          files,
          Sources.main(web, name + "Handler"),
          "entity/" + architecture + "handler_java.jte",
          model);
    }
    if (serviceTests) {
      once(
          files,
          Sources.test(application, name + "ServiceTests"),
          "entity/" + architecture + "service_tests_java.jte",
          tests);
    }
    if (apiTests) {
      once(files, Sources.test(web, name + "ApiTests"), "entity/api_tests_java.jte", tests);
    }
  }

  /**
   * The prefix of the templates of the service, its tests and the handler in the architecture of
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
    files.own(path, PlainPack.TEMPLATES.render(path, EVERY_RUN, template, parameters));
  }

  /** Writes a file of the developer's: once. */
  private static void once(
      ProjectFiles files, String path, String template, Map<String, Object> parameters) {
    files.once(path, PlainPack.TEMPLATES.render(path, ONCE, template, parameters));
  }
}
