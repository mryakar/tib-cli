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

/** How the identifier of an entity gets its value. */
public enum IdStrategy {
  /** The database assigns it, from an identity column; for a {@code Long}. */
  IDENTITY("identity"),
  /** A database sequence assigns it; for a {@code Long}. */
  SEQUENCE("sequence"),
  /** The application generates it; for a {@code UUID}. */
  UUID("uuid");

  private final String name;

  IdStrategy(String name) {
    this.name = name;
  }

  /** The name in the spec. */
  @Override
  public String toString() {
    return name;
  }

  /** Whether the strategy produces values of the type. */
  public boolean fits(IdType type) {
    return (this == UUID) == (type == IdType.UUID);
  }

  /** The strategy an identifier of the type gets when the spec names none. */
  public static IdStrategy defaultFor(IdType type) {
    return type == IdType.UUID ? UUID : IDENTITY;
  }
}
