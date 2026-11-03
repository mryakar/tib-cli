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

  /** The length of a String that the spec gives none. */
  static final int DEFAULT_LENGTH = 255;

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

  /** The length of a String: the spec's, or else the default; a field of another type has none. */
  Integer length() {
    if (field.length() != null || field.type() != FieldType.STRING) {
      return field.length();
    }
    return DEFAULT_LENGTH;
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
    if (length() != null) {
      column.append(", length = ").append(length());
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
  String validation() {
    StringBuilder validation = new StringBuilder();
    if (!field.nullable()) {
      validation.append("@NotNull ");
    }
    if (length() != null) {
      validation.append("@Size(max = ").append(length()).append(") ");
    }
    return validation.toString();
  }

  /**
   * A value of the field for the tests, as Java: the first or the second, which differ. A String
   * fits its length, and a BigDecimal its precision and scale, so that it reads back as it was.
   */
  String sample(int variant) {
    return switch (field.type()) {
      case STRING -> {
        String text = variant == 1 ? "One" : "Two";
        yield "\"" + text.substring(0, Math.min(text.length(), length())) + "\"";
      }
      case INTEGER -> String.valueOf(variant);
      case LONG -> variant + "L";
      case BIG_DECIMAL -> "new BigDecimal(\"" + decimal(variant) + "\")";
      case BOOLEAN -> String.valueOf(variant == 1);
      case LOCAL_DATE -> "LocalDate.of(2026, 1, " + variant + ")";
      case LOCAL_DATE_TIME -> "LocalDateTime.of(2026, 1, " + variant + ", 12, 0)";
      case INSTANT -> "Instant.parse(\"2026-01-0" + variant + "T12:00:00Z\")";
      case UUID -> uuid(variant);
    };
  }

  /** A UUID for the tests, as Java. */
  static String uuid(int variant) {
    return "UUID.fromString(\"00000000-0000-0000-0000-00000000000" + variant + "\")";
  }

  /**
   * The text of a BigDecimal for the tests, with the scale of the column: {@code 1.00}, or {@code
   * 0.01} when the precision leaves no digit before the point.
   */
  private String decimal(int variant) {
    int scale = field.scale() == null ? 0 : field.scale();
    if (scale == 0) {
      return String.valueOf(variant);
    }
    if (field.precision() != null && field.precision() == scale) {
      return "0." + "0".repeat(scale - 1) + variant;
    }
    return variant + "." + "0".repeat(scale);
  }

  /** The definition of the column in SQL. */
  String sql() {
    StringBuilder sql =
        new StringBuilder(
            field.column()
                + " "
                + sqlType(field.type(), length(), field.precision(), field.scale()));
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

  /** The SQL type of a Java type. */
  private static String sqlType(FieldType type, Integer length, Integer precision, Integer scale) {
    return switch (type) {
      case STRING -> "VARCHAR(" + length + ")";
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
