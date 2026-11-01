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

import me.yakar.tib.spec.Field;
import me.yakar.tib.spec.FieldType;
import me.yakar.tib.spec.IdType;
import me.yakar.tib.spec.Names;

/** A field as the templates see it. */
public record FieldModel(Field field) {

  public String name() {
    return field.name();
  }

  public String javaType() {
    return field.type().toString();
  }

  /** The name with its first letter in upper case, for the accessors. */
  public String property() {
    return Names.property(field.name());
  }

  boolean nullable() {
    return field.nullable();
  }

  Integer length() {
    return field.length();
  }

  /** The JPA annotation of the column. */
  public String columnAnnotation() {
    StringBuilder column = new StringBuilder("@Column(name = \"" + field.column() + "\"");
    if (!field.nullable()) {
      column.append(", nullable = false");
    }
    if (field.unique()) {
      column.append(", unique = true");
    }
    if (field.length() != null) {
      column.append(", length = ").append(field.length());
    }
    if (field.precision() != null) {
      column.append(", precision = ").append(field.precision());
    }
    if (field.scale() != null) {
      column.append(", scale = ").append(field.scale());
    }
    return column.append(")").toString();
  }

  /** The validation annotations of the DTO's component, in front of its type. */
  public String validation() {
    StringBuilder validation = new StringBuilder();
    if (!field.nullable()) {
      validation.append("@NotNull ");
    }
    if (field.length() != null) {
      validation.append("@Size(max = ").append(field.length()).append(") ");
    }
    return validation.toString();
  }

  /** The definition of the column in SQL. */
  public String sql() {
    StringBuilder sql =
        new StringBuilder(
            field.column()
                + " "
                + sqlType(field.type(), field.length(), field.precision(), field.scale()));
    if (!field.nullable()) {
      sql.append(" NOT NULL");
    }
    if (field.unique()) {
      sql.append(" UNIQUE");
    }
    return sql.toString();
  }

  /** The SQL type of an identifier, and of a column that refers to one. */
  static String sqlType(IdType type) {
    return sqlType(type == IdType.UUID ? FieldType.UUID : FieldType.LONG, null, null, null);
  }

  /** The SQL type of a Java type: a String without a length gets 255. */
  static String sqlType(FieldType type, Integer length, Integer precision, Integer scale) {
    return switch (type) {
      case STRING -> "VARCHAR(" + (length == null ? 255 : length) + ")";
      case INTEGER -> "INTEGER";
      case LONG -> "BIGINT";
      case BIG_DECIMAL ->
          precision == null
              ? "NUMERIC"
              : "NUMERIC(" + precision + ", " + (scale == null ? 0 : scale) + ")";
      case BOOLEAN -> "BOOLEAN";
      case LOCAL_DATE -> "DATE";
      case LOCAL_DATE_TIME -> "TIMESTAMP";
      case INSTANT -> "TIMESTAMP WITH TIME ZONE";
      case UUID -> "UUID";
    };
  }
}
