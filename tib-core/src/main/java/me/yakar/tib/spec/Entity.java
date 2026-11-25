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

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    fields = Spec.list(fields);
    relations = Spec.list(relations);
    audit = audit != null && audit;
    options = options == null ? Options.defaults() : options;
  }

  /**
   * The segment of the package of the entity, under the package of the service: the name in lower
   * case, as in {@code vehicle}.
   */
  public String packageSegment() {
    return name.toLowerCase(Locale.ROOT);
  }

  void check(Problems problems, String path, Map<String, Entity> entities) {
    if (Names.isKeyword(packageSegment())) {
      problems.add(
          path + ".name",
          "'" + name + "' makes the package '" + packageSegment() + "', which is a Java keyword");
    }
    problems.sqlName(path, "table", table, "entity");
    id.check(problems, path + ".id");
    Members members = new Members(problems, path, id, audit);
    for (int i = 0; i < fields.size(); i++) {
      members.field(i, fields.get(i));
    }
    for (int i = 0; i < relations.size(); i++) {
      members.relation(i, relations.get(i), entities);
    }
    members.targetIds(entities);
  }
}
