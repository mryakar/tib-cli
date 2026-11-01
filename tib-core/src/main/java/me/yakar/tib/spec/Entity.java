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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

  void check(Problems problems, String path, Set<String> entityNames) {
    id.check(problems, path + ".id");
    Set<String> members = new HashSet<>();
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
        problems.add(fieldPath + ".name", "'" + field.name() + "' is already a member");
      } else {
        column(problems, fieldPath, field.column(), columns);
      }
    }
    for (int i = 0; i < relations.size(); i++) {
      Relation relation = relations.get(i);
      String relationPath = path + ".relations[" + i + "]";
      if (!entityNames.contains(relation.target())) {
        problems.add(relationPath + ".target", "there is no entity '" + relation.target() + "'");
      }
      if (!members.add(relation.field())) {
        problems.add(relationPath + ".field", "'" + relation.field() + "' is already a member");
      } else {
        column(problems, relationPath, relation.column(), columns);
      }
    }
  }

  /** The column of a member, which no other member of the entity may have. */
  private static void column(Problems problems, String path, String column, Set<String> columns) {
    if (!columns.add(column.toUpperCase(Locale.ROOT))) {
      problems.add(path + ".column", "'" + column + "' is already a column");
    }
  }
}
