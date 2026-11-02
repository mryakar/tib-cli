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
package me.yakar.tib.spec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read from a YAML file, the spec carries the defaults for what the file left out, so that the same
 * spec means the same thing everywhere.
 */
@Doc("The spec of a service: the service, its group, its pack and its entities.")
public record Spec(
    @Doc(
            value = "The version of the spec format; " + Spec.VERSION + ".",
            required = true,
            minimum = Spec.VERSION,
            maximum = Spec.VERSION)
        Integer tib,
    @Doc(
            value = "The name of the service, in lower case words joined with hyphens.",
            required = true,
            pattern = Names.SERVICE)
        String service,
    @Doc(
            value = "The group of the service: the root of its packages and its Maven group.",
            pattern = Names.GROUP,
            defaultValue = "com.example")
        String group,
    @Doc(
            value = "The template pack, by name, with its version after an at sign to pin it.",
            pattern = Names.PACK,
            defaultValue = "default")
        String pack,
    @Doc("The entities of the service.") List<Entity> entities) {

  /** The version of the spec format that this build reads. */
  public static final int VERSION = 1;

  // The marks of an entity while the relations are followed for cycles.
  private static final int NEW = 0;
  private static final int VISITING = 1;
  private static final int DONE = 2;

  public Spec {
    group = group == null ? "com.example" : group;
    pack = pack == null ? "default" : pack;
    entities = Problems.list(entities);
  }

  /**
   * Checks the spec against its rules: the structure first, then what the values mean together.
   *
   * @return the spec
   * @throws SpecException with every problem found
   */
  public Spec check() {
    Problems problems = new Problems();
    problems.check(this, "");
    if (problems.isEmpty()) {
      check(problems);
    }
    if (!problems.isEmpty()) {
      throw new SpecException(problems.messages());
    }
    return this;
  }

  private void check(Problems problems) {
    for (String segment : group.split("\\.")) {
      if (Names.isKeyword(segment)) {
        problems.add("group", "'" + group + "' has '" + segment + "', which is a Java keyword");
      }
    }
    String packageName = service.replace("-", "");
    if (Names.isKeyword(packageName)) {
      problems.add(
          "service",
          "'" + service + "' makes the package '" + packageName + "', which is a Java keyword");
    }
    // The entities by name; the first one of a name.
    Map<String, Entity> names = new HashMap<>();
    // The tables, in upper case: SQL names are the same in any case.
    Set<String> tables = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      Entity entity = entities.get(i);
      if (names.putIfAbsent(entity.name(), entity) != null) {
        problems.add("entities[" + i + "].name", "'" + entity.name() + "' is already an entity");
      } else if (!tables.add(entity.table().toUpperCase(Locale.ROOT))) {
        problems.add("entities[" + i + "].table", "'" + entity.table() + "' is already a table");
      }
    }
    for (int i = 0; i < entities.size(); i++) {
      entities.get(i).check(problems, "entities[" + i + "]", names);
    }
    Map<String, Integer> indexes = new HashMap<>();
    for (int i = 0; i < entities.size(); i++) {
      indexes.putIfAbsent(entities.get(i).name(), i);
    }
    int[] marks = new int[entities.size()];
    Map<String, String> cycles = new LinkedHashMap<>();
    for (int i = 0; i < entities.size(); i++) {
      if (marks[i] == NEW) {
        visit(i, indexes, marks, new ArrayList<>(), cycles);
      }
    }
    cycles.forEach(problems::add);
  }

  /**
   * Follows the relations of the entity, depth first, and keeps each cycle they close by the path
   * of the relation that leaves the entity where the cycle starts. A relation of an entity to
   * itself is no cycle.
   *
   * @param path the relations followed to the entity, each as the index of its entity and its own
   */
  private void visit(
      int entity,
      Map<String, Integer> indexes,
      int[] marks,
      List<int[]> path,
      Map<String, String> cycles) {
    marks[entity] = VISITING;
    List<Relation> relations = entities.get(entity).relations();
    for (int i = 0; i < relations.size(); i++) {
      Integer target = indexes.get(relations.get(i).target());
      if (target == null || target == entity) {
        continue;
      }
      path.add(new int[] {entity, i});
      if (marks[target] == VISITING) {
        int[] start = path.stream().filter(step -> step[0] == target).findFirst().orElseThrow();
        Entity from = entities.get(start[0]);
        cycles.putIfAbsent(
            "entities[" + start[0] + "].relations[" + start[1] + "].target",
            "'"
                + from.relations().get(start[1]).target()
                + "' refers back to '"
                + from.name()
                + "'; a cycle of relations is not supported");
      } else if (marks[target] == NEW) {
        visit(target, indexes, marks, path, cycles);
      }
      path.remove(path.size() - 1);
    }
    marks[entity] = DONE;
  }
}
