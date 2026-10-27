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

  /** The member name of a type name: {@code Depot} is {@code depot}. */
  public static String member(String typeName) {
    return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
  }
}
