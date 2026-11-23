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
package me.yakar.tib.pack;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.Relation;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates the entity layer of every entity of the spec, with its tests, each entity after the
 * entities it refers to, so that its migration can reference their tables.
 */
final class EntityGenerator implements Generator {

  static final String MIGRATIONS = "src/main/resources/db/migration";

  /** The header of a file of tib's. */
  private static final String EVERY_RUN =
      "Written by tib on every run. Do not edit: your changes would be lost.";

  /** The header of a file of the developer's. */
  private static final String ONCE =
      "Written by tib when the entity was created; it is yours from then on.";

  /** The header of a file of the developer's that the service needs once it has entities. */
  private static final String FIRST_ENTITY =
      "Written by tib with the first entity of the service; it is yours from then on.";

  /** The header of a migration, which is the developer's. */
  private static final String MIGRATION =
      "Written by tib when the entity was created. A change of the schema is a new migration.";

  /**
   * A versioned migration, at any depth: the major part of its version, as in V2__, V2.1__, V2_1__.
   */
  private static final Pattern VERSION = Pattern.compile("/V(\\d+)(?:[._]\\d+)*__");

  @Override
  public void generate(Project project, ProjectFiles files) {
    Map<String, EntityModel> models = models(project);
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
    String path = DefaultPack.MAIN + project.packagePath() + "/" + type + ".java";
    files.once(path, Templates.render(path, FIRST_ENTITY, template, Map.of("project", project)));
  }

  /** The models of the entities of the project, by name, each after the entities it refers to. */
  static Map<String, EntityModel> models(Project project) {
    Map<String, EntityModel> models = new LinkedHashMap<>();
    for (Entity entity : ordered(project.spec().entities())) {
      models.put(entity.name(), new EntityModel(project, entity, models));
    }
    return models;
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
    String migration = migration(files, entity);
    files.lasting(
        migration, Templates.render(migration, MIGRATION, "entity/migration_sql.jte", model));
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
    files.own(path, Templates.render(path, EVERY_RUN, template, parameters));
  }

  /** Writes a file of the developer's: once. */
  private static void once(
      ProjectFiles files, String path, String template, Map<String, Object> parameters) {
    files.once(path, Templates.render(path, ONCE, template, parameters));
  }

  /**
   * The migration that creates the table: the one tib wrote before, there or not, so that a
   * migration the developer removed or renamed stays so; else the one there; else the next major
   * version after the versioned migrations there.
   */
  private static String migration(ProjectFiles files, EntityModel entity) {
    String suffix = "__create_" + entity.table().toLowerCase(Locale.ROOT) + ".sql";
    for (String migration : files.recorded(MIGRATIONS)) {
      if (migration.endsWith(suffix)) {
        return migration;
      }
    }
    long latest = 0;
    for (String migration : files.list(MIGRATIONS)) {
      if (migration.endsWith(suffix)) {
        return migration;
      }
      Matcher version = VERSION.matcher(migration);
      if (version.find()) {
        latest = Math.max(latest, Long.parseLong(version.group(1)));
      }
    }
    return MIGRATIONS + "/V" + (latest + 1) + suffix;
  }

  /**
   * The entities, each after the entities it refers to; otherwise in the order of the spec. A spec
   * has no cycle of relations, but an entity may refer to itself.
   */
  static List<Entity> ordered(List<Entity> entities) {
    Map<String, Entity> byName = new LinkedHashMap<>();
    entities.forEach(entity -> byName.put(entity.name(), entity));
    Set<String> done = new LinkedHashSet<>();
    byName.keySet().forEach(name -> visit(name, byName, done));
    return done.stream().map(byName::get).toList();
  }

  private static void visit(String name, Map<String, Entity> entities, Set<String> done) {
    if (done.contains(name)) {
      return;
    }
    for (Relation relation : entities.get(name).relations()) {
      if (!relation.target().equals(name)) {
        visit(relation.target(), entities, done);
      }
    }
    done.add(name);
  }
}
