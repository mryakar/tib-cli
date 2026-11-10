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
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.Relation;
import me.yakar.tib.tree.Tree;

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
  public void generate(Project project, Tree tree) {
    Map<String, EntityModel> models = models(project);
    models.values().forEach(model -> generateEntity(model, tree));
    // Once, by the run that first generates an API: a service scaffolded before it gets it too.
    if (models.values().stream().anyMatch(EntityModel::layer)) {
      String handler = DefaultPack.MAIN + project.packagePath() + "/ConflictHandler.java";
      once(
          tree,
          handler,
          FIRST_ENTITY,
          "entity/conflict_handler_java.jte",
          Map.of("project", project));
    }
  }

  /** The models of the entities of the project, by name, each after the entities it refers to. */
  static Map<String, EntityModel> models(Project project) {
    Map<String, EntityModel> models = new LinkedHashMap<>();
    for (Entity entity : ordered(project.spec().entities())) {
      models.put(entity.name(), new EntityModel(project, entity, models));
    }
    return models;
  }

  private static void generateEntity(EntityModel entity, Tree tree) {
    String main = entity.directory() + "/" + entity.name();
    String test = entity.testDirectory() + "/" + entity.name();
    Map<String, Object> model = Map.of("entity", entity);
    Map<String, Object> tests = Map.of("tests", new EntityTestsModel(entity));
    String generated = EVERY_RUN + " Your code goes in " + entity.name() + ".";
    own(tree, main + "Generated.java", generated, "entity/generated_java.jte", model);
    once(tree, main + ".java", ONCE, "entity/entity_java.jte", model);
    once(tree, main + "Repository.java", ONCE, "entity/repository_java.jte", model);
    if (entity.dto()) {
      own(tree, main + "Dto.java", EVERY_RUN, "entity/dto_java.jte", model);
    }
    // A migration lasts: a database may have run it, so it stays when the entity leaves the spec.
    String migration = migration(tree, entity);
    tree.lasting(
        migration, Templates.render(migration, MIGRATION, "entity/migration_sql.jte", model));
    once(tree, test + "RepositoryTests.java", ONCE, "entity/repository_tests_java.jte", tests);
    if (entity.layer()) {
      own(tree, main + "Mapper.java", EVERY_RUN, "entity/mapper_java.jte", model);
      once(tree, main + "Service.java", ONCE, "entity/service_java.jte", model);
      once(tree, main + "Controller.java", ONCE, "entity/controller_java.jte", model);
      once(tree, test + "ServiceTests.java", ONCE, "entity/service_tests_java.jte", tests);
      once(tree, test + "ApiTests.java", ONCE, "entity/api_tests_java.jte", tests);
    }
  }

  /** Writes a file of tib's: on every run. */
  private static void own(
      Tree tree, String path, String header, String template, Map<String, Object> parameters) {
    tree.own(path, Templates.render(path, header, template, parameters));
  }

  /** Writes a file of the developer's: once. */
  private static void once(
      Tree tree, String path, String header, String template, Map<String, Object> parameters) {
    tree.once(path, Templates.render(path, header, template, parameters));
  }

  /**
   * The migration that creates the table: the one tib wrote before, there or not, so that a
   * migration the developer removed or renamed stays so; else the one there; else the next major
   * version after the versioned migrations there.
   */
  private static String migration(Tree tree, EntityModel entity) {
    String suffix = "__create_" + entity.table().toLowerCase(Locale.ROOT) + ".sql";
    for (String migration : tree.recorded(MIGRATIONS)) {
      if (migration.endsWith(suffix)) {
        return migration;
      }
    }
    long latest = 0;
    for (String migration : tree.list(MIGRATIONS)) {
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
