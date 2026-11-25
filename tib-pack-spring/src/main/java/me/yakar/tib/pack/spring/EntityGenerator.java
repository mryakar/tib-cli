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
import me.yakar.tib.pack.GeneratedFiles;
import me.yakar.tib.pack.Migration;
import me.yakar.tib.pack.Sources;
import me.yakar.tib.pack.Templates;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates the entity layer of every entity of the spec, with its tests, each entity after the
 * entities it refers to, so that its migration can reference their tables.
 */
final class EntityGenerator implements Generator {

  @Override
  public void generate(Project project, ProjectFiles files) {
    Map<String, EntityModel> models = EntityModel.models(project);
    GeneratedFiles generated = new GeneratedFiles(SpringPack.TEMPLATES, files);
    models.values().forEach(model -> generateEntity(project, model, generated, files));
    // Written here and not by init: only a service with an API needs it.
    if (models.values().stream().anyMatch(EntityModel::layer)) {
      generated.onceWithFirstEntity(project, "ConflictHandler", "entity/conflict_handler_java.jte");
      // The services of DDD and hexagonal throw it, and the handler answers it: they know no HTTP.
      if (project.architecture() != Architecture.ANEMIC) {
        generated.onceWithFirstEntity(
            project, "NotFoundException", "entity/not_found_exception_java.jte");
      }
    }
  }

  private static void generateEntity(
      Project project, EntityModel entity, GeneratedFiles generated, ProjectFiles files) {
    String name = entity.name();
    String domain = entity.domainPackage();
    String application = entity.applicationPackage();
    String web = entity.webPackage();
    String persistence = entity.persistencePackage();
    // DDD and hexagonal have services of their own: the use cases of the domain, which know no DTO.
    String architecture = Templates.prefix(project.architecture());
    JpaModel jpa = new JpaModel(entity);
    Map<String, Object> model = Map.of("entity", entity, "jpa", jpa, "dto", new DtoModel(entity));
    Map<String, Object> tests = Map.of("tests", new EntityTestsModel(entity), "jpa", jpa);
    if (entity.hexagonal()) {
      // The domain knows no JPA: the table is mapped by a class of the persistence adapter, which
      // keeps the domain through the store, the port of the application.
      generated.own(
          Sources.main(domain, name + "Generated"),
          "entity/hexagonal_domain_generated_java.jte",
          model);
      generated.onceWithEntity(
          Sources.main(domain, name), "entity/hexagonal_domain_java.jte", model);
      generated.own(
          Sources.main(persistence, jpa.entityClass()), "entity/generated_java.jte", model);
      generated.own(
          Sources.main(persistence, name + "PersistenceMapper"),
          "entity/hexagonal_persistence_mapper_java.jte",
          model);
      generated.onceWithEntity(
          Sources.main(entity.storePackage(), name + "Store"),
          "entity/hexagonal_store_java.jte",
          model);
      generated.onceWithEntity(
          Sources.main(persistence, name + "PersistenceAdapter"),
          "entity/hexagonal_persistence_adapter_java.jte",
          model);
    } else {
      generated.own(Sources.main(domain, name + "Generated"), "entity/generated_java.jte", model);
      generated.onceWithEntity(Sources.main(domain, name), "entity/entity_java.jte", model);
    }
    generated.onceWithEntity(
        Sources.main(persistence, jpa.repository()), "entity/repository_java.jte", model);
    if (entity.dto()) {
      generated.own(Sources.main(web, name + "Dto"), "entity/dto_java.jte", model);
    }
    Migration.write(files, entity, JpaModel.ALLOCATION_SIZE);
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
          Sources.main(web, name + "Controller"),
          "entity/" + architecture + "controller_java.jte",
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
}
