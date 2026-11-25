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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The members and the columns of an entity while it is checked: the ones of its identifier first,
 * then the ones of its fields and of its relations as they are given. A member or a column that is
 * there already is a problem.
 */
final class Members {

  private final Problems problems;

  /** The path of the entity. */
  private final String path;

  /** The members that the generated code has whatever the spec says, each with its problem. */
  private final Map<String, String> generated = new HashMap<>();

  private final Set<String> members = new HashSet<>();

  /** The columns, by the keys of their names. */
  private final Set<String> columns = new HashSet<>();

  /** The relations whose field is a member of its own, by path. */
  private final Map<String, Relation> relations = new LinkedHashMap<>();

  /** The members of an entity at the path, with those of its identifier. */
  Members(Problems problems, String path, Id id, boolean audit) {
    this.problems = problems;
    this.path = path;
    if (!id.field().equals(Id.FIELD)) {
      generated.put(
          Id.FIELD,
          "'id' is already a member: the accessors of the identifier are getId and setId");
    }
    if (audit) {
      generated.put("createdAt", "'createdAt' is already a member of an audited entity");
      generated.put("updatedAt", "'updatedAt' is already a member of an audited entity");
      columns.addAll(List.of(Entity.CREATED_AT, Entity.UPDATED_AT));
    }
    members.addAll(generated.keySet());
    members.add(id.field());
    if (generated.containsKey(id.field())) {
      problems.add(path + ".id.field", generated.get(id.field()));
    } else {
      column(path + ".id", id.column());
    }
  }

  /** Checks the field, and takes its member and its column. */
  void field(int index, Field field) {
    String fieldPath = path + ".fields[" + index + "]";
    field.check(problems, fieldPath);
    if (!members.add(field.name())) {
      problems.add(fieldPath + ".name", taken(field.name()));
    } else {
      column(fieldPath, field.column());
    }
  }

  /**
   * Checks the relation, and takes its member and its column.
   *
   * @param entities the entities of the spec, by name
   */
  void relation(int index, Relation relation, Map<String, Entity> entities) {
    String relationPath = path + ".relations[" + index + "]";
    relation.check(problems, relationPath);
    if (!entities.containsKey(relation.target())) {
      problems.add(relationPath + ".target", "there is no entity '" + relation.target() + "'");
    }
    if (!members.add(relation.field())) {
      problems.add(relationPath + ".field", taken(relation.field()));
    } else {
      relations.put(relationPath, relation);
      column(relationPath, relation.column());
    }
  }

  /**
   * Takes the member in which the DTO holds the identifier of the target of each relation, as in
   * {@code depotId}, after every field and relation has its own.
   *
   * @param entities the entities of the spec, by name
   */
  void targetIds(Map<String, Entity> entities) {
    relations.forEach(
        (relationPath, relation) -> {
          Entity target = entities.get(relation.target());
          if (target != null) {
            String member = relation.field() + Names.property(target.id().field());
            if (!members.add(member)) {
              problems.add(
                  relationPath + ".field",
                  "'"
                      + member
                      + "', which holds the identifier of the target, is already a member");
            }
          }
        });
  }

  private String taken(String member) {
    return generated.getOrDefault(member, "'" + member + "' is already a member");
  }

  /** The column of a member, which no other member of the entity may have. */
  private void column(String memberPath, String column) {
    if (!columns.add(Names.key(column))) {
      problems.add(memberPath + ".column", "'" + column + "' is already a column");
    }
  }
}
