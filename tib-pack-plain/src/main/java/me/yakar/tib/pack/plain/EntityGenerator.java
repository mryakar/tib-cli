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
import me.yakar.tib.pack.GeneratedFiles;
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

  @Override
  public void generate(Project project, ProjectFiles files) {
    Map<String, EntityModel> models = EntityModel.models(project, true);
    GeneratedFiles generated = new GeneratedFiles(PlainPack.TEMPLATES, files);
    models.values().forEach(model -> generateEntity(project, model, generated, files));
    // Written here and not by init: only a service with an API needs them.
    if (models.values().stream().anyMatch(EntityModel::layer)) {
      generated.onceWithFirstEntity(
          project, "NotFoundException", "entity/not_found_exception_java.jte");
      generated.onceWithFirstEntity(
          project, "BadRequestException", "entity/bad_request_exception_java.jte");
      generated.onceWithFirstEntity(project, "ErrorHandler", "entity/error_handler_java.jte");
      generated.onceWithFirstEntity(project, "Http", "entity/http_java.jte");
    }
    // The repository of an entity whose list comes in pages gives one.
    if (models.values().stream().anyMatch(EntityModel::paginated)) {
      generated.onceWithFirstEntity(project, "Page", "entity/page_java.jte");
    }
    routes(project, models.values(), generated);
  }

  /**
   * Writes the routes of the API, which tib keeps in step with the entities: the classes of every
   * entity with a service and a handler, wired by hand.
   */
  static void routes(Project project, Collection<EntityModel> entities, GeneratedFiles generated) {
    generated.own(
        Sources.main(project.basePackage(), "Routes"),
        "entity/routes_java.jte",
        Map.of(
            "project", project, "entities", entities.stream().filter(EntityModel::layer).toList()));
  }

  private static void generateEntity(
      Project project, EntityModel entity, GeneratedFiles generated, ProjectFiles files) {
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
    generated.own(Sources.main(domain, name + "Generated"), "entity/generated_java.jte", model);
    generated.onceWithEntity(Sources.main(domain, name), "entity/entity_java.jte", model);
    // The repository is the SQL of the table that the spec makes, so it follows the spec.
    generated.own(
        Sources.main(persistence, name + "Repository"), "entity/repository_java.jte", model);
    if (entity.hexagonal()) {
      // The application keeps the entity through the store, its port, which the persistence
      // adapter implements on the repository.
      generated.onceWithEntity(
          Sources.main(entity.storePackage(), name + "Store"),
          "entity/hexagonal_store_java.jte",
          model);
      generated.onceWithEntity(
          Sources.main(persistence, name + "PersistenceAdapter"),
          "entity/hexagonal_persistence_adapter_java.jte",
          model);
    }
    if (entity.dto()) {
      generated.own(Sources.main(web, name + "Dto"), "entity/dto_java.jte", model);
    }
    Migration.write(files, entity);
    // The tests of the kinds of the spec; the service and the API only of an entity with a layer.
    boolean repositoryTests = project.tests(TestKind.INTEGRATION);
    boolean serviceTests = entity.layer() && project.tests(TestKind.UNIT);
    boolean apiTests = entity.layer() && project.tests(TestKind.API);
    if (repositoryTests || serviceTests || apiTests) {
      generated.own(
          Sources.test(entity.packageName(), name + "Samples"), "entity/samples_java.jte", tests);
    }
    if (repositoryTests && entity.hexagonal()) {
      generated.onceWithEntity(
          Sources.test(persistence, name + "PersistenceAdapterTests"),
          "entity/hexagonal_persistence_adapter_tests_java.jte",
          tests);
    } else if (repositoryTests) {
      generated.onceWithEntity(
          Sources.test(persistence, name + "RepositoryTests"),
          "entity/repository_tests_java.jte",
          tests);
    }
    if (entity.layer()) {
      generated.own(Sources.main(web, entity.webMapper()), "entity/mapper_java.jte", model);
      if (entity.hexagonal()) {
        generated.onceWithEntity(
            Sources.main(entity.useCasePackage(), name + "UseCase"),
            "entity/hexagonal_use_case_java.jte",
            model);
      }
      generated.onceWithEntity(
          Sources.main(application, name + "Service"),
          "entity/" + architecture + "service_java.jte",
          model);
      generated.onceWithEntity(
          Sources.main(web, name + "Handler"),
          "entity/" + architecture + "handler_java.jte",
          model);
    }
    if (serviceTests) {
      generated.onceWithEntity(
          Sources.test(application, name + "ServiceTests"),
          "entity/" + architecture + "service_tests_java.jte",
          tests);
    }
    if (apiTests) {
      generated.onceWithEntity(
          Sources.test(web, name + "ApiTests"), "entity/api_tests_java.jte", tests);
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
}
