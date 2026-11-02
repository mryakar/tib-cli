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

import java.util.HashMap;
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
 * Generates the entity layer of every entity of the spec, each entity after the entities it refers
 * to, so that its migration can reference their tables.
 */
final class EntityGenerator implements Generator {

  static final String MIGRATIONS = "src/main/resources/db/migration";

  /**
   * A versioned migration, at any depth: the major part of its version, as in V2__, V2.1__, V2_1__.
   */
  private static final Pattern VERSION = Pattern.compile("/V(\\d+)(?:[._]\\d+)*__");

  @Override
  public void generate(Project project, Tree tree) {
    Map<String, EntityModel> models = new HashMap<>();
    for (Entity entity : ordered(project.spec().entities())) {
      EntityModel model = new EntityModel(project, entity, models);
      models.put(entity.name(), model);
      generateEntity(model, tree);
    }
  }

  private static void generateEntity(EntityModel entity, Tree tree) {
    String directory = entity.directory() + "/";
    String name = entity.name();
    tree.own(directory + name + "Generated.java", render("entity/generated_java.jte", entity));
    tree.once(directory + name + ".java", render("entity/entity_java.jte", entity));
    tree.once(directory + name + "Repository.java", render("entity/repository_java.jte", entity));
    if (entity.dto()) {
      tree.own(directory + name + "Dto.java", render("entity/dto_java.jte", entity));
    }
    if (entity.layer()) {
      tree.own(directory + name + "Mapper.java", render("entity/mapper_java.jte", entity));
      tree.once(directory + name + "Service.java", render("entity/service_java.jte", entity));
      tree.once(directory + name + "Controller.java", render("entity/controller_java.jte", entity));
    }
    tree.once(migration(tree, entity), render("entity/migration_sql.jte", entity));
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

  private static String render(String template, EntityModel entity) {
    return Templates.render(template, Map.of("entity", entity));
  }
}
