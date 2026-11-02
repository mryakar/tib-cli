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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Doc("An entity: a table and the class mapped to it.")
public record Entity(
    @Doc(
            value = "The name of the entity, in upper camel case.",
            required = true,
            pattern = Names.TYPE)
        String name,
    @Doc(
            value = "The name of the table. Without one, the entity's name in upper snake case.",
            pattern = Names.SQL)
        String table,
    @Doc("The identifier. Without one, a Long named id that the database assigns.") Id id,
    @Doc("The fields of the entity.") List<Field> fields,
    @Doc("The relations of the entity to others.") List<Relation> relations,
    @Doc(
            value =
                "Whether the entity records when it was created and last changed, in the columns"
                    + " CREATED_AT and UPDATED_AT.",
            defaultValue = "false")
        Boolean audit,
    @Doc("What is generated for the entity.") Options options) {

  /** The columns of an audited entity. */
  public static final String CREATED_AT = "CREATED_AT", UPDATED_AT = "UPDATED_AT";

  public Entity {
    table = table == null && name != null ? Names.sql(name) : table;
    id = id == null ? Id.defaults() : id;
    fields = Problems.list(fields);
    relations = Problems.list(relations);
    audit = audit != null && audit;
    options = options == null ? Options.defaults() : options;
  }

  void check(Problems problems, String path, Map<String, Entity> entities) {
    String packageName = name.toLowerCase(Locale.ROOT);
    if (Names.isKeyword(packageName)) {
      problems.add(
          path + ".name",
          "'" + name + "' makes the package '" + packageName + "', which is a Java keyword");
    }
    if (Names.isReserved(table)) {
      problems.add(
          path + ".table", "'" + table + "' is a reserved word in SQL; give the entity a table");
    }
    id.check(problems, path + ".id");
    // The members that the generated code has whatever the spec says, each with its problem.
    Map<String, String> generated = new HashMap<>();
    if (!id.field().equals("id")) {
      generated.put(
          "id", "'id' is already a member: the accessors of the identifier are getId and setId");
    }
    if (audit) {
      generated.put("createdAt", "'createdAt' is already a member of an audited entity");
      generated.put("updatedAt", "'updatedAt' is already a member of an audited entity");
    }
    if (generated.containsKey(id.field())) {
      problems.add(path + ".id.field", generated.get(id.field()));
    }
    Set<String> members = new HashSet<>(generated.keySet());
    members.add(id.field());
    // The columns, in upper case: SQL names are the same in any case.
    Set<String> columns = new HashSet<>();
    columns.add(Names.sql(id.field()));
    if (audit) {
      columns.addAll(List.of(CREATED_AT, UPDATED_AT));
    }
    for (int i = 0; i < fields.size(); i++) {
      Field field = fields.get(i);
      String fieldPath = path + ".fields[" + i + "]";
      field.check(problems, fieldPath);
      if (!members.add(field.name())) {
        problems.add(fieldPath + ".name", taken(field.name(), generated));
      } else {
        column(problems, fieldPath, field.column(), columns);
      }
    }
    // The relations whose field is a member of its own, by path.
    Map<String, Relation> accepted = new LinkedHashMap<>();
    for (int i = 0; i < relations.size(); i++) {
      Relation relation = relations.get(i);
      String relationPath = path + ".relations[" + i + "]";
      relation.check(problems, relationPath);
      if (!entities.containsKey(relation.target())) {
        problems.add(relationPath + ".target", "there is no entity '" + relation.target() + "'");
      }
      if (!members.add(relation.field())) {
        problems.add(relationPath + ".field", taken(relation.field(), generated));
      } else {
        accepted.put(relationPath, relation);
        column(problems, relationPath, relation.column(), columns);
      }
    }
    // The DTO holds the identifier of a relation's target in a member of its own: depotId.
    accepted.forEach(
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

  private static String taken(String member, Map<String, String> generated) {
    return generated.getOrDefault(member, "'" + member + "' is already a member");
  }

  /** The column of a member, which no other member of the entity may have. */
  private static void column(Problems problems, String path, String column, Set<String> columns) {
    if (!columns.add(column.toUpperCase(Locale.ROOT))) {
      problems.add(path + ".column", "'" + column + "' is already a column");
    }
  }
}
