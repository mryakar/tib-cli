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
package me.yakar.tib.ddl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.yakar.tib.spec.FieldType;
import net.sf.jsqlparser.statement.create.table.ColDataType;

/**
 * The type of a field that an SQL type maps to, with its sizes; {@code serial} when the type has
 * the database assign the value, as {@code BIGSERIAL} does.
 */
record SqlType(FieldType type, Integer length, Integer precision, Integer scale, boolean serial) {

  /** The name of a type, its sizes in parentheses and what follows them. */
  private static final Pattern TYPE = Pattern.compile("([^(]*)(?:\\(([^)]*)\\))?(.*)");

  /** A size, which may have a unit after it, as in {@code VARCHAR2(20 CHAR)}. */
  private static final Pattern SIZE = Pattern.compile("\\s*(\\d+)(\\s+\\w+)?\\s*");

  /** The SQL type as the DDL writes it, for the messages. */
  static String text(ColDataType type) {
    List<String> arguments = type.getArgumentsStringList();
    String text =
        type.getDataType() + (arguments == null ? "" : "(" + String.join(", ", arguments) + ")");
    return text.replaceAll("\\s+\\(", "(");
  }

  /** The type of the field that the SQL type maps to; null for a type that no field type holds. */
  static SqlType of(ColDataType type) {
    if (!type.getArrayData().isEmpty()) {
      return null;
    }
    Matcher text = TYPE.matcher(text(type));
    text.matches();
    String name =
        (text.group(1) + " " + text.group(3))
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    List<Integer> sizes = new ArrayList<>();
    if (text.group(2) != null) {
      for (String size : text.group(2).split(",")) {
        Matcher digits = SIZE.matcher(size);
        if (!digits.matches()) {
          return null;
        }
        sizes.add(Integer.valueOf(digits.group(1)));
      }
    }
    Integer first = sizes.isEmpty() ? null : sizes.get(0);
    return switch (name) {
      case "VARCHAR",
          "VARCHAR2",
          "NVARCHAR",
          "NVARCHAR2",
          "CHARACTER VARYING",
          "CHAR",
          "CHARACTER",
          "NCHAR",
          "TEXT",
          "CLOB",
          "NCLOB" ->
          new SqlType(FieldType.STRING, first, null, null, false);
      case "SMALLINT", "INT", "INTEGER", "INT2", "INT4" -> plain(FieldType.INTEGER);
      case "SMALLSERIAL", "SERIAL" -> new SqlType(FieldType.INTEGER, null, null, null, true);
      case "BIGINT", "INT8" -> plain(FieldType.LONG);
      case "BIGSERIAL" -> new SqlType(FieldType.LONG, null, null, null, true);
      case "NUMERIC", "DECIMAL", "NUMBER" -> number(sizes);
      case "BOOLEAN", "BOOL" -> plain(FieldType.BOOLEAN);
      case "DATE" -> plain(FieldType.LOCAL_DATE);
      case "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE", "DATETIME" ->
          plain(FieldType.LOCAL_DATE_TIME);
      case "TIMESTAMPTZ", "TIMESTAMP WITH TIME ZONE" -> plain(FieldType.INSTANT);
      case "UUID" -> plain(FieldType.UUID);
      default -> null;
    };
  }

  private static SqlType plain(FieldType type) {
    return new SqlType(type, null, null, null, false);
  }

  /**
   * A number without a fraction is an Integer up to nine digits and a Long up to nineteen, as the
   * NUMBER(19) of a key; any other number is a BigDecimal.
   */
  private static SqlType number(List<Integer> sizes) {
    if (sizes.isEmpty()) {
      return plain(FieldType.BIG_DECIMAL);
    }
    int precision = sizes.get(0);
    int scale = sizes.size() > 1 ? sizes.get(1) : 0;
    if (scale == 0 && precision <= 9) {
      return plain(FieldType.INTEGER);
    }
    if (scale == 0 && precision <= 19) {
      return plain(FieldType.LONG);
    }
    return new SqlType(FieldType.BIG_DECIMAL, null, precision, scale, false);
  }
}
