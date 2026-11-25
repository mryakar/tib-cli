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
import me.yakar.tib.spec.Names;

/** A field as the templates see it. */
public record FieldModel(Field field) {

  /** The length of a String whose spec gives none. */
  static final int DEFAULT_LENGTH = 255;

  public String name() {
    return field.name();
  }

  public FieldType type() {
    return field.type();
  }

  public String column() {
    return field.column();
  }

  /** The simple name of the Java type of the value: a Text is a String. */
  public String javaType() {
    return switch (field.type()) {
      case TEXT -> "String";
      default -> field.type().toString();
    };
  }

  /** The import of the Java type, as a line of Java; nothing for a type of {@code java.lang}. */
  public String typeImport() {
    return switch (field.type()) {
      case BIG_DECIMAL -> "import java.math.BigDecimal;";
      case LOCAL_DATE -> "import java.time.LocalDate;";
      case LOCAL_DATE_TIME -> "import java.time.LocalDateTime;";
      case INSTANT -> "import java.time.Instant;";
      case UUID -> "import java.util.UUID;";
      case STRING, TEXT, INTEGER, LONG, BOOLEAN -> "";
    };
  }

  /** Whether the field is a Text: a String that the database keeps without a limit. */
  public boolean text() {
    return field.type() == FieldType.TEXT;
  }

  /** The name with its first letter in upper case, for the accessors. */
  public String property() {
    return Names.property(field.name());
  }

  public boolean nullable() {
    return field.nullable();
  }

  public boolean unique() {
    return field.unique();
  }

  /** The precision of a BigDecimal, or null. */
  public Integer precision() {
    return field.precision();
  }

  /** The scale of a BigDecimal, or null. */
  public Integer scale() {
    return field.scale();
  }

  /** The length of a String: the spec's, or else the default; a field of another type has none. */
  public Integer length() {
    if (field.type() == FieldType.STRING && field.length() == null) {
      return DEFAULT_LENGTH;
    }
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
  public String sample(int variant) {
    return switch (field.type()) {
      case STRING, TEXT -> {
        String text = variant == 1 ? "One" : "Two";
        yield "\""
            + (length() == null ? text : text.substring(0, Math.min(text.length(), length())))
            + "\"";
      }
      case INTEGER -> String.valueOf(variant);
      case LONG -> variant + "L";
      case BIG_DECIMAL -> "new BigDecimal(\"" + decimal(variant) + "\")";
      case BOOLEAN -> String.valueOf(variant == 1);
      case LOCAL_DATE -> "LocalDate.of(2026, 1, " + variant + ")";
      case LOCAL_DATE_TIME -> "LocalDateTime.of(2026, 1, " + variant + ", 12, 0)";
      case INSTANT -> "Instant.parse(\"2026-01-0" + variant + "T12:00:00Z\")";
      case UUID -> sampleUuid(variant);
    };
  }

  /** A UUID for the tests, as Java. */
  static String sampleUuid(int variant) {
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
}
