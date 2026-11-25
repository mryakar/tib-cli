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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read from a YAML file, the spec carries the defaults for what the file left out, so that the same
 * spec means the same thing everywhere.
 */
@Doc(
    "The spec of a service: the service, its group, its pack, its Java, its architecture, its"
        + " tests, whether it has a Dockerfile, and its entities.")
public record Spec(
    @Doc(
            value = "The version of the spec format; " + Spec.VERSION + ".",
            required = true,
            minimum = Spec.VERSION,
            maximum = Spec.VERSION)
        Integer tib,
    @Doc(
            value = "The name of the service, in lower case words joined with hyphens.",
            required = true,
            pattern = Names.SERVICE)
        String service,
    @Doc(
            value = "The group of the service: the root of its packages and its Maven group.",
            pattern = Names.GROUP,
            defaultValue = Spec.GROUP)
        String group,
    @Doc(
            value =
                "The template pack, default or plain, with its version after an at sign to pin it.",
            pattern = Names.PACK,
            defaultValue = Spec.PACK)
        String pack,
    @Doc(
            value =
                "The Java version of the service: the one its build compiles for, and the one it"
                    + " runs on.",
            values = {21, 25},
            defaultValue = "" + Spec.JAVA)
        Integer java,
    @Doc(value = "How the code of the service is arranged.", defaultValue = "anemic")
        Architecture architecture,
    @Doc(
            "The kinds of tests that tib writes: unit, integration, api and archunit; unit,"
                + " integration and api when the spec gives none, and none when it gives an empty"
                + " list.")
        List<TestKind> tests,
    @Doc(
            value = "Whether the service has a Dockerfile that builds it into an image.",
            defaultValue = "false")
        Boolean docker,
    @Doc("The entities of the service.") List<Entity> entities) {

  /** The version of the spec format that this build reads. */
  static final int VERSION = 1;

  /** The group of a service whose spec gives none. */
  static final String GROUP = "com.example";

  /** The pack of a service whose spec gives none. */
  static final String PACK = "default";

  /** The kinds of tests of a service whose spec gives none. */
  static final List<TestKind> TESTS = List.of(TestKind.UNIT, TestKind.INTEGRATION, TestKind.API);

  /** The Java version of a service whose spec gives none. */
  static final int JAVA = 21;

  public Spec {
    group = group == null ? GROUP : group;
    pack = pack == null ? PACK : pack;
    java = java == null ? JAVA : java;
    architecture = architecture == null ? Architecture.ANEMIC : architecture;
    tests = tests == null ? TESTS : list(tests);
    docker = docker != null && docker;
    entities = list(entities);
  }

  /**
   * The spec of a service that has only its name, checked.
   *
   * @throws SpecException when the name is not one of a service
   */
  public static Spec of(String service) {
    return of(service, null, null, null, null, null);
  }

  /**
   * The spec of a service that has only its name, its pack, its Java version, its architecture, its
   * kinds of tests and whether it has a Dockerfile, checked.
   *
   * @param pack the pack, or null for the default
   * @param java the Java version, or null for the default
   * @param architecture the architecture, or null for the default
   * @param tests the kinds of tests, or null for the default
   * @param docker whether the service has a Dockerfile, or null for the default
   * @throws SpecException when the name is not one of a service, the pack not one of a pack, the
   *     version not one of Java that tib generates for, or a kind of test is there twice
   */
  public static Spec of(
      String service,
      String pack,
      Integer java,
      Architecture architecture,
      List<TestKind> tests,
      Boolean docker) {
    return new Spec(VERSION, service, null, pack, java, architecture, tests, docker, null).check();
  }

  /** The spec with these entities in place of its own. */
  public Spec withEntities(List<Entity> entities) {
    return new Spec(tib, service, group, pack, java, architecture, tests, docker, entities);
  }

  /**
   * The list as the model keeps it: never null, never changed. A null item stays, for the check to
   * report.
   */
  public static <T> List<T> list(List<T> list) {
    return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
  }

  /**
   * The segment of the packages that the service adds to its group: the name without hyphens, as in
   * {@code vehicleservice}.
   */
  public String packageSegment() {
    return service.replace("-", "");
  }

  /**
   * Checks the spec against its rules: the structure first, then what the values mean together.
   *
   * @return the spec
   * @throws SpecException with every problem found
   */
  public Spec check() {
    Problems problems = new Problems();
    problems.check(this, "");
    if (problems.isEmpty()) {
      check(problems);
    }
    if (!problems.isEmpty()) {
      throw new SpecException(problems.messages());
    }
    return this;
  }

  private void check(Problems problems) {
    for (String segment : group.split("\\.")) {
      if (Names.isKeyword(segment)) {
        problems.add("group", "'" + group + "' has '" + segment + "', which is a Java keyword");
      }
    }
    Set<TestKind> kinds = EnumSet.noneOf(TestKind.class);
    for (int i = 0; i < tests.size(); i++) {
      if (!kinds.add(tests.get(i))) {
        problems.add("tests[" + i + "]", "'" + tests.get(i) + "' is already there");
      }
    }
    if (Names.isKeyword(packageSegment())) {
      problems.add(
          "service",
          "'"
              + service
              + "' makes the package '"
              + packageSegment()
              + "', which is a Java keyword");
    }
    // The entities by name, and their indexes; the first one of a name.
    Map<String, Entity> names = new HashMap<>();
    Map<String, Integer> indexes = new HashMap<>();
    // The tables, by the keys of their names.
    Set<String> tables = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      Entity entity = entities.get(i);
      if (names.putIfAbsent(entity.name(), entity) != null) {
        problems.add("entities[" + i + "].name", "'" + entity.name() + "' is already an entity");
      } else {
        indexes.put(entity.name(), i);
        if (!tables.add(Names.key(entity.table()))) {
          problems.add("entities[" + i + "].table", "'" + entity.table() + "' is already a table");
        }
      }
    }
    for (int i = 0; i < entities.size(); i++) {
      entities.get(i).check(problems, "entities[" + i + "]", names);
    }
    Cycles.problems(entities, indexes).forEach(problems::add);
  }
}
