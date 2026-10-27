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

/** A field of an entity, with the column that holds it. */
@Doc("A field of the entity, with the column that holds it.")
public record Field(
    @Doc(
            value = "The name of the field, in lower camel case.",
            required = true,
            pattern = Names.MEMBER)
        String name,
    @Doc(
            value = "The name of the column. Without one, the field's name in upper snake case.",
            pattern = Names.SQL)
        String column,
    @Doc(value = "The Java type of the field.", required = true) FieldType type,
    @Doc(value = "The length of a String; the pack picks one when there is none.", minimum = 1)
        Integer length,
    @Doc(value = "The precision of a BigDecimal: its number of digits.", minimum = 1)
        Integer precision,
    @Doc(value = "The scale of a BigDecimal: its number of digits after the point.", minimum = 0)
        Integer scale,
    @Doc(value = "Whether the column accepts null.", defaultValue = "true") Boolean nullable,
    @Doc(value = "Whether the column is unique.", defaultValue = "false") Boolean unique) {

  public Field {
    column = column == null && name != null ? Names.sql(name) : column;
    nullable = nullable == null || nullable;
    unique = unique != null && unique;
  }

  void check(Problems problems, String path) {
    if (length != null && type != FieldType.STRING) {
      problems.add(path + ".length", "only a String has a length");
    }
    if ((precision != null || scale != null) && type != FieldType.BIG_DECIMAL) {
      problems.add(path, "only a BigDecimal has a precision and a scale");
    }
    if (precision != null && scale != null && scale > precision) {
      problems.add(path + ".scale", "the scale is above the precision");
    }
  }
}
