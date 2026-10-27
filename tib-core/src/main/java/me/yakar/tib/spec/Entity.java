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
import java.util.Set;

/** An entity: a table and the Java class mapped to it, with its fields and relations. */
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
            value = "Whether the entity records when and by whom it was created and changed.",
            defaultValue = "false")
        Boolean audit,
    @Doc("What is generated for the entity.") Options options) {

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
    for (int i = 0; i < fields.size(); i++) {
      Field field = fields.get(i);
      String fieldPath = path + ".fields[" + i + "]";
      field.check(problems, fieldPath);
      if (!members.add(field.name())) {
        problems.add(fieldPath + ".name", "'" + field.name() + "' is already a member");
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
      }
    }
  }
}
