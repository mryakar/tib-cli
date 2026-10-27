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

  /** The problems of the spec; none when it is valid. */
  List<String> problems() {
    Problems problems = new Problems();
    problems.check(this, "");
    if (problems.isEmpty()) {
      check(problems);
    }
    return problems.messages();
  }

  private void check(Problems problems) {
    if (tib != VERSION) {
      problems.add("tib", "this build reads version " + VERSION);
    }
    Set<String> names = new HashSet<>();
    for (int i = 0; i < entities.size(); i++) {
      if (!names.add(entities.get(i).name())) {
        problems.add(
            "entities[" + i + "].name", "'" + entities.get(i).name() + "' is already an entity");
      }
    }
    for (int i = 0; i < entities.size(); i++) {
      entities.get(i).check(problems, "entities[" + i + "]", names);
    }
  }
}
