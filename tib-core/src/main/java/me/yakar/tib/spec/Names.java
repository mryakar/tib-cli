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

import java.util.Locale;
import java.util.Set;
import javax.lang.model.SourceVersion;

/** The patterns that the names in a spec follow, and the conversions between Java and SQL names. */
public final class Names {

  /** A service name: lower case words joined with hyphens, as in {@code vehicle-service}. */
  static final String SERVICE = "^[a-z][a-z0-9]*(-[a-z0-9]+)*$";

  /** A group: the root of the packages of a project, as in {@code com.example}. */
  static final String GROUP = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$";

  /** A pack: its name, optionally with its version after an at sign. */
  static final String PACK = "^[a-z][a-z0-9-]*(@[0-9A-Za-z.+-]+)?$";

  /** A Java type name: upper camel case. */
  static final String TYPE = "^[A-Z][A-Za-z0-9]*$";

  /** A Java member name: lower camel case. */
  static final String MEMBER = "^[a-z][A-Za-z0-9]*$";

  /**
   * An SQL identifier: letters, digits and underscores. It needs no quoting unless it is a reserved
   * word, which {@link #isReserved} tells.
   */
  static final String SQL = "^[A-Za-z_][A-Za-z0-9_]*$";

  /**
   * The words that PostgreSQL reserves, most of them reserved in standard SQL too: a table or a
   * column cannot have one as its name without quotes.
   */
  private static final Set<String> RESERVED =
      Set.of(
          "ALL",
          "ANALYSE",
          "ANALYZE",
          "AND",
          "ANY",
          "ARRAY",
          "AS",
          "ASC",
          "ASYMMETRIC",
          "AUTHORIZATION",
          "BINARY",
          "BOTH",
          "CASE",
          "CAST",
          "CHECK",
          "COLLATE",
          "COLLATION",
          "COLUMN",
          "CONCURRENTLY",
          "CONSTRAINT",
          "CREATE",
          "CROSS",
          "CURRENT_CATALOG",
          "CURRENT_DATE",
          "CURRENT_ROLE",
          "CURRENT_SCHEMA",
          "CURRENT_TIME",
          "CURRENT_TIMESTAMP",
          "CURRENT_USER",
          "DEFAULT",
          "DEFERRABLE",
          "DESC",
          "DISTINCT",
          "DO",
          "ELSE",
          "END",
          "EXCEPT",
          "FALSE",
          "FETCH",
          "FOR",
          "FOREIGN",
          "FREEZE",
          "FROM",
          "FULL",
          "GRANT",
          "GROUP",
          "HAVING",
          "ILIKE",
          "IN",
          "INITIALLY",
          "INNER",
          "INTERSECT",
          "INTO",
          "IS",
          "ISNULL",
          "JOIN",
          "LATERAL",
          "LEADING",
          "LEFT",
          "LIKE",
          "LIMIT",
          "LOCALTIME",
          "LOCALTIMESTAMP",
          "NATURAL",
          "NOT",
          "NOTNULL",
          "NULL",
          "OFFSET",
          "ON",
          "ONLY",
          "OR",
          "ORDER",
          "OUTER",
          "OVERLAPS",
          "PLACING",
          "PRIMARY",
          "REFERENCES",
          "RETURNING",
          "RIGHT",
          "SELECT",
          "SESSION_USER",
          "SIMILAR",
          "SOME",
          "SYMMETRIC",
          "SYSTEM_USER",
          "TABLE",
          "TABLESAMPLE",
          "THEN",
          "TO",
          "TRAILING",
          "TRUE",
          "UNION",
          "UNIQUE",
          "USER",
          "USING",
          "VARIADIC",
          "VERBOSE",
          "WHEN",
          "WHERE",
          "WINDOW",
          "WITH");

  private Names() {}

  /** Whether a Java name is a keyword of the language, as {@code class} is. */
  static boolean isKeyword(String javaName) {
    return !SourceVersion.isName(javaName);
  }

  /**
   * Whether an SQL name, in any case, is a word that the database reserves, as {@code ORDER} is.
   */
  static boolean isReserved(String sqlName) {
    return RESERVED.contains(sqlName.toUpperCase(Locale.ROOT));
  }

  /**
   * The SQL name of a Java name, when the spec gives none: {@code plateNumber} is {@code
   * PLATE_NUMBER}.
   */
  public static String sql(String javaName) {
    StringBuilder sql = new StringBuilder();
    for (int i = 0; i < javaName.length(); i++) {
      char c = javaName.charAt(i);
      if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(javaName.charAt(i - 1))) {
        sql.append('_');
      }
      sql.append(Character.toUpperCase(c));
    }
    return sql.toString();
  }

  /**
   * The plural of a member name, by the common rules of English: {@code branch}, {@code branches}.
   */
  public static String plural(String name) {
    if (name.endsWith("s")
        || name.endsWith("x")
        || name.endsWith("z")
        || name.endsWith("ch")
        || name.endsWith("sh")) {
      return name + "es";
    }
    if (name.endsWith("y")
        && name.length() > 1
        && "aeiou".indexOf(name.charAt(name.length() - 2)) < 0) {
      return name.substring(0, name.length() - 1) + "ies";
    }
    return name + "s";
  }

  /**
   * The Java type name of an SQL name: {@code VEHICLE_TYPE} and {@code vehicle_type} are {@code
   * VehicleType}; a word in mixed case keeps its case, as in {@code vehicleType}.
   */
  public static String type(String sqlName) {
    StringBuilder type = new StringBuilder();
    for (String word : sqlName.split("_")) {
      if (!word.isEmpty()) {
        String cased =
            word.equals(word.toUpperCase(Locale.ROOT)) || word.equals(word.toLowerCase(Locale.ROOT))
                ? word.toLowerCase(Locale.ROOT)
                : word;
        type.append(Character.toUpperCase(cased.charAt(0))).append(cased.substring(1));
      }
    }
    return type.toString();
  }

  /**
   * The singular of a name, by the common rules of English: {@code Depots} is {@code Depot}, {@code
   * Branches} is {@code Branch}, {@code Statuses} is {@code Status}, {@code Houses} is {@code
   * House}; a name that does not end as a plural is one already, as {@code Status} is. English is
   * not that regular, so some names come out wrong, as {@code Movies}, which is {@code Movy}; the
   * spec can name such an entity itself.
   */
  public static String singular(String name) {
    if (name.endsWith("uses")) {
      // After a consonant it is the plural of -us, as in Statuses; else of -use, as in Houses.
      boolean us = name.length() > 4 && "aeiou".indexOf(name.charAt(name.length() - 5)) < 0;
      return name.substring(0, name.length() - (us ? 2 : 1));
    }
    if (name.endsWith("ies")) {
      return name.substring(0, name.length() - 3) + "y";
    }
    if (name.endsWith("sses")
        || name.endsWith("xes")
        || name.endsWith("zes")
        || name.endsWith("ches")
        || name.endsWith("shes")) {
      return name.substring(0, name.length() - 2);
    }
    if (name.endsWith("s")
        && !name.endsWith("ss")
        && !name.endsWith("us")
        && !name.endsWith("is")) {
      return name.substring(0, name.length() - 1);
    }
    return name;
  }

  /** The member name of a type name: {@code Depot} is {@code depot}. */
  public static String member(String typeName) {
    return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
  }

  /** The accessor name of a member name: {@code plateNumber} is {@code PlateNumber}. */
  public static String property(String member) {
    return Character.toUpperCase(member.charAt(0)) + member.substring(1);
  }
}
