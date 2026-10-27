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

/** The patterns that the names in a spec follow. */
public final class Names {

  /** A service name: lower case words joined with hyphens, as in {@code vehicle-service}. */
  public static final String SERVICE = "^[a-z][a-z0-9]*(-[a-z0-9]+)*$";

  /** A group: the root of the packages of a project, as in {@code com.example}. */
  public static final String GROUP = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$";

  /** A pack: its name, optionally with its version after an at sign. */
  public static final String PACK = "^[a-z][a-z0-9-]*(@[0-9A-Za-z.+-]+)?$";

  /** A Java type name: upper camel case. */
  public static final String TYPE = "^[A-Z][A-Za-z0-9]*$";

  /** A Java member name: lower camel case. */
  public static final String MEMBER = "^[a-z][A-Za-z0-9]*$";

  /** An SQL identifier that needs no quoting. */
  public static final String SQL = "^[A-Za-z_][A-Za-z0-9_]*$";

  private Names() {}

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
        && !"aeiou".contains(name.substring(name.length() - 2, name.length() - 1))) {
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
   * The singular of a name, by the rules of {@link #plural}: {@code Depots} is {@code Depot},
   * {@code Branches} is {@code Branch}; a name that does not end as a plural is one already, as
   * {@code Status} is.
   */
  public static String singular(String name) {
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
}
