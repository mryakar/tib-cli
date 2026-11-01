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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The spec of a service: what tib generates, resolved. Read from a YAML file, it carries the
 * defaults for what the file left out, so that the same spec means the same thing everywhere.
 */
@Doc("The spec of a service: the service, its group, its pack and its entities.")
public record Spec(
    @Doc(value = "The version of the spec format; 1.", required = true, minimum = 1) Integer tib,
    @Doc(
            value = "The name of the service, in lower case words joined with hyphens.",
            required = true,
            pattern = Names.SERVICE)
        String service,
    @Doc(
            value = "The group of the service: the root of its packages and its Maven group.",
            pattern = Names.GROUP,
            defaultValue = "com.example")
        String group,
    @Doc(
            value = "The template pack, by name, with its version after an at sign to pin it.",
            pattern = Names.PACK,
            defaultValue = "default")
        String pack,
    @Doc("The entities of the service.") List<Entity> entities) {

  /** The version of the spec format that this build reads. */
  public static final int VERSION = 1;

  public Spec {
    group = group == null ? "com.example" : group;
    pack = pack == null ? "default" : pack;
    entities = Problems.list(entities);
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
    String packageName = service.replace("-", "");
    if (Names.isKeyword(packageName)) {
      problems.add(
          "service",
          "'" + service + "' makes the package '" + packageName + "', which is a Java keyword");
    }
    if (tib != VERSION) {
      problems.add("tib", "this build reads version " + VERSION);
    }
    Set<String> names = new HashSet<>();
    // The tables, in upper case: SQL names are the same in any case.
    Set<String> tables = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      Entity entity = entities.get(i);
      if (!names.add(entity.name())) {
        problems.add("entities[" + i + "].name", "'" + entity.name() + "' is already an entity");
      } else if (!tables.add(entity.table().toUpperCase(Locale.ROOT))) {
        problems.add("entities[" + i + "].table", "'" + entity.table() + "' is already a table");
      }
    }
    for (int i = 0; i < entities.size(); i++) {
      entities.get(i).check(problems, "entities[" + i + "]", names);
    }
  }
}
