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

/** A kind of test that tib writes for the entities of a service. */
public enum TestKind {
  /** The tests of the service of an entity, with its repository and its mapper mocked. */
  UNIT("unit"),

  /**
   * The tests of the repository of an entity, and the test of the application, against the
   * database.
   */
  INTEGRATION("integration"),

  /** The tests of the API of an entity, against the running service and its database. */
  API("api"),

  /** The test of the rules of the architecture of the service, on its classes. */
  ARCHUNIT("archunit");

  private final String name;

  TestKind(String name) {
    this.name = name;
  }

  /** The name in the spec. */
  @Override
  public String toString() {
    return name;
  }
}
