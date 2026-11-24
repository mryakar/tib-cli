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

import java.util.ArrayList;
import java.util.List;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.TestKind;

/** The kinds of tests of a service, as the notes for agents tell of them. */
public final class TestsModel {

  private final Project project;

  public TestsModel(Project project) {
    this.project = project;
  }

  /** Whether the entities of the service have tests of any kind. */
  public boolean any() {
    return project.tests(TestKind.UNIT)
        || project.tests(TestKind.INTEGRATION)
        || project.tests(TestKind.API);
  }

  /** Whether the service has the test of its architecture. */
  public boolean archunit() {
    return project.tests(TestKind.ARCHUNIT);
  }

  /** The rules that the test of the architecture checks, in words, for the notes for agents. */
  public String rules() {
    return switch (project.architecture()) {
      case ANEMIC ->
          "the controller of an entity over its service over its\nrepository, and no cycle between"
              + " the packages of the entities";
      case DDD ->
          "the domain of an entity uses neither its application nor its web, the application does"
              + "\nnot use the web, the domain of an aggregate does not use the domain of another, and"
              + "\nthere is no cycle between the packages of the entities";
      case HEXAGONAL ->
          "the domain knows neither JPA nor Spring, the application does"
              + "\nnot use the adapters, an inbound adapter does not use an outbound one, nor the reverse, and"
              + "\nthere is no cycle between the packages of the entities";
    };
  }

  /** Whether the service has tests that start PostgreSQL in a container. */
  public boolean container() {
    return project.tests(TestKind.INTEGRATION) || project.tests(TestKind.API);
  }

  /**
   * The tests of an entity {@code X} in words, as in {@code `XServiceTests` with the repository and
   * the mapper mocked}; the ones against the database come first, and a line feed comes before the
   * unit tests when there are both. In the hexagonal architecture, the store takes the place of the
   * repository.
   */
  public String entityTests() {
    boolean hexagonal = project.architecture() == Architecture.HEXAGONAL;
    List<String> container = new ArrayList<>();
    if (project.tests(TestKind.INTEGRATION)) {
      container.add(hexagonal ? "`XPersistenceAdapterTests`" : "`XRepositoryTests`");
    }
    if (project.tests(TestKind.API)) {
      container.add("`XApiTests`");
    }
    String database = String.join(" and ", container) + " against PostgreSQL in a container";
    String unit =
        hexagonal
            ? "`XServiceTests` with the store mocked"
            : "`XServiceTests` with the repository and the mapper mocked";
    if (!project.tests(TestKind.UNIT)) {
      return database;
    }
    return container.isEmpty() ? unit : database + ", and\n" + unit;
  }
}
