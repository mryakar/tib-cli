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

@Doc("The identifier of the entity.")
public record Id(
    @Doc(value = "The name of the field.", pattern = Names.MEMBER, defaultValue = Id.FIELD)
        String field,
    @Doc(value = "The Java type of the identifier.", defaultValue = "Long") IdType type,
    @Doc(
            value =
                "How the identifier gets its value: identity or sequence for a Long, uuid for a UUID.")
        IdStrategy strategy) {

  /** The field of an identifier whose spec gives none. */
  static final String FIELD = "id";

  public Id {
    field = field == null ? FIELD : field;
    type = type == null ? IdType.LONG : type;
    strategy = strategy == null ? IdStrategy.defaultFor(type) : strategy;
  }

  /** The column of the identifier: the field in upper snake case. */
  public String column() {
    return Names.sql(field);
  }

  /** The identifier an entity gets when the spec gives none. */
  static Id defaults() {
    return new Id(null, null, null);
  }

  void check(Problems problems, String path) {
    if (Names.isKeyword(field)) {
      problems.add(path + ".field", "'" + field + "' is a Java keyword");
    }
    if (Names.isReserved(column())) {
      problems.add(
          path + ".field",
          "the column '"
              + column()
              + "' is a reserved word in SQL; give the identifier another field");
    }
    if (!strategy.fits(type)) {
      problems.add(path, "the strategy " + strategy + " does not fit the type " + type);
    }
  }
}
