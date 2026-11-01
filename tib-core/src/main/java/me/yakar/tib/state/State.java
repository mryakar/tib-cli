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
package me.yakar.tib.state;

import java.util.List;
import java.util.Optional;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.tree.Owner;
import me.yakar.tib.tree.Tree;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * What tib keeps in the project about what it generated: the version of tib and the pack it ran
 * with, the resolved spec, and every file it wrote with its owner and the hash of its content. The
 * file is {@value #PATH}; tib writes it, nobody edits it.
 *
 * @param tib the version of tib that wrote the state
 */
public record State(String tib, PackRef pack, Spec spec, List<File> files) {

  /** The path of the state file in a project. */
  public static final String PATH = ".tib/state.yaml";

  private static final YAMLMapper YAML =
      YAMLMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
          .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
          .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
          .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
          .build();

  /** The pack that generated the files. */
  public record PackRef(String name, String version) {}

  /** A file that tib wrote, with its owner and the SHA-256 of its content when it was written. */
  public record File(String path, Owner owner, String sha256) {}

  public State {
    files = files == null ? List.of() : List.copyOf(files);
  }

  /** The state file of the project in the tree, if tib ran on it before. */
  public static Optional<State> in(Tree tree) {
    return tree.read(PATH).map(State::parse);
  }

  /** The state as YAML. */
  public String yaml() {
    return YAML.writeValueAsString(this);
  }

  /** The state read from its YAML, with its spec checked as a spec file is. */
  public static State parse(String yaml) {
    State state;
    try {
      state = YAML.readValue(yaml, State.class);
    } catch (JacksonException e) {
      throw new StateException(PATH + ": " + e.getOriginalMessage());
    }
    if (state == null || state.spec() == null) {
      throw new StateException(PATH + ": spec: required");
    }
    state.spec().check();
    return state;
  }
}
