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

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.util.ArrayList;
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
 * Generates the entity layer of every entity of the spec: the mapped superclass with the columns,
 * written on every run, and the entity, written once; the repository; the DTO and the mapper; the
 * service and the controller, written once; and the Flyway migration that creates the table,
 * written once and numbered after the migrations already there. Entities come after the entities
 * they refer to, so that the migrations can reference their tables.
 */
final class EntityGenerator implements Generator {

  static final String MIGRATIONS = "src/main/resources/db/migration";

  private static final TemplateEngine TEMPLATES =
      TemplateEngine.createPrecompiled(ContentType.Plain);
  private static final Pattern VERSION = Pattern.compile("/V(\\d+)__");

  @Override
  public void generate(Project project, Tree tree) {
    Map<String, Entity> entities = new LinkedHashMap<>();
    project.spec().entities().forEach(entity -> entities.put(entity.name(), entity));
    for (Entity entity : ordered(entities)) {
      generate(new EntityModel(project, entity, entities), tree);
    }
  }

  private void generate(EntityModel entity, Tree tree) {
    String directory = entity.directory() + "/";
    String name = entity.name();
    tree.own(directory + name + "Generated.java", render("entity/generated_java.jte", entity));
    tree.once(directory + name + ".java", render("entity/entity_java.jte", entity));
    tree.own(directory + name + "Repository.java", render("entity/repository_java.jte", entity));
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

  /** The migration that creates the table: the one already there, or the next one. */
  static String migration(Tree tree, EntityModel entity) {
    String suffix = "__create_" + entity.table().toLowerCase(Locale.ROOT) + ".sql";
    int latest = 0;
    for (String migration : tree.list(MIGRATIONS)) {
      if (migration.endsWith(suffix)) {
        return migration;
      }
      Matcher version = VERSION.matcher(migration);
      if (version.find()) {
        latest = Math.max(latest, Integer.parseInt(version.group(1)));
      }
    }
    return MIGRATIONS + "/V" + (latest + 1) + suffix;
  }

  /** The entities, each after the entities it refers to; otherwise in the order of the spec. */
  static List<Entity> ordered(Map<String, Entity> entities) {
    Set<String> done = new LinkedHashSet<>();
    entities.keySet().forEach(name -> visit(name, entities, new LinkedHashSet<>(), done));
    List<Entity> ordered = new ArrayList<>();
    done.forEach(name -> ordered.add(entities.get(name)));
    return ordered;
  }

  private static void visit(
      String name, Map<String, Entity> entities, Set<String> visiting, Set<String> done) {
    // An entity that is placed, or is being placed (a cycle, or a relation to itself), is done.
    if (done.contains(name) || !visiting.add(name)) {
      return;
    }
    for (Relation relation : entities.get(name).relations()) {
      visit(relation.target(), entities, visiting, done);
    }
    done.add(name);
  }

  private static String render(String template, EntityModel entity) {
    StringOutput output = new StringOutput();
    TEMPLATES.render(template, Map.of("entity", entity), output);
    return output.toString();
  }
}
