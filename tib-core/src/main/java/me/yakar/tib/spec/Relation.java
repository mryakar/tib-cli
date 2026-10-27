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

/** A relation from an entity to another, held by a column on the declaring entity's table. */
@Doc("A relation to another entity, held by a column of this entity's table.")
public record Relation(
    @Doc(value = "The kind of the relation.", required = true) RelationKind kind,
    @Doc(value = "The name of the target entity.", required = true, pattern = Names.TYPE)
        String target,
    @Doc(
            value =
                "The name of the field that holds the target. Without one, the target's name in lower camel case.",
            pattern = Names.MEMBER)
        String field,
    @Doc(
            value =
                "The name of the column. Without one, the field's name in upper snake case with _ID.",
            pattern = Names.SQL)
        String column,
    @Doc(
            value = "Whether the relation may be missing: whether the column accepts null.",
            defaultValue = "true")
        Boolean nullable) {

  public Relation {
    field = field == null && target != null ? Names.member(target) : field;
    column = column == null && field != null ? Names.sql(field) + "_ID" : column;
    nullable = nullable == null || nullable;
  }
}
