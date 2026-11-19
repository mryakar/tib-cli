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

/**
 * The type of a field: the Java type of its value, or {@code Text}, a String without a limit. The
 * pack maps it to the SQL type of the column.
 */
public enum FieldType {
  STRING("String"),
  TEXT("Text"),
  INTEGER("Integer"),
  LONG("Long"),
  BIG_DECIMAL("BigDecimal"),
  BOOLEAN("Boolean"),
  LOCAL_DATE("LocalDate"),
  LOCAL_DATE_TIME("LocalDateTime"),
  INSTANT("Instant"),
  UUID("UUID");

  private final String name;

  FieldType(String name) {
    this.name = name;
  }

  /** The name in the spec. */
  @Override
  public String toString() {
    return name;
  }
}
