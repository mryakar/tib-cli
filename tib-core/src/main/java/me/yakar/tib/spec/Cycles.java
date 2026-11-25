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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The search for the cycles that the relations of the entities of a spec close, depth first. A
 * relation of an entity to itself is no cycle.
 */
final class Cycles {

  /** Where the search is with an entity. */
  private enum Mark {
    NEW,
    VISITING,
    DONE
  }

  /** A relation that the search followed: the index of its entity, and its own in the entity. */
  private record Step(int entity, int relation) {}

  private final List<Entity> entities;
  private final Map<String, Integer> indexes;
  private final Mark[] marks;

  /** The relations followed to the entity that is visited. */
  private final List<Step> path = new ArrayList<>();

  private final Map<String, String> problems = new LinkedHashMap<>();

  private Cycles(List<Entity> entities, Map<String, Integer> indexes) {
    this.entities = entities;
    this.indexes = indexes;
    marks = new Mark[entities.size()];
    Arrays.fill(marks, Mark.NEW);
  }

  /**
   * The cycles of the entities, each as a problem by the path of the relation that leaves the
   * entity where the cycle starts, in the order in which they are found.
   *
   * @param indexes the indexes of the entities, by name
   */
  static Map<String, String> problems(List<Entity> entities, Map<String, Integer> indexes) {
    Cycles cycles = new Cycles(entities, indexes);
    for (int i = 0; i < entities.size(); i++) {
      if (cycles.marks[i] == Mark.NEW) {
        cycles.visit(i);
      }
    }
    return cycles.problems;
  }

  /** Follows the relations of the entity, and keeps each cycle they close once. */
  private void visit(int entity) {
    marks[entity] = Mark.VISITING;
    List<Relation> relations = entities.get(entity).relations();
    for (int i = 0; i < relations.size(); i++) {
      Integer target = indexes.get(relations.get(i).target());
      if (target == null || target == entity) {
        continue;
      }
      path.add(new Step(entity, i));
      if (marks[target] == Mark.VISITING) {
        Step start =
            path.stream().filter(step -> step.entity() == target).findFirst().orElseThrow();
        Entity from = entities.get(start.entity());
        problems.putIfAbsent(
            "entities[" + start.entity() + "].relations[" + start.relation() + "].target",
            "'"
                + from.relations().get(start.relation()).target()
                + "' refers back to '"
                + from.name()
                + "'; a cycle of relations is not supported");
      } else if (marks[target] == Mark.NEW) {
        visit(target);
      }
      path.remove(path.size() - 1);
    }
    marks[entity] = Mark.DONE;
  }
}
