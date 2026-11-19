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
package me.yakar.tib.cli;

import java.nio.file.Path;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecReader;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;

/**
 * The spec file that {@code --spec} names: the spec in it, and the file as the state records it.
 */
record SpecFile(Spec spec, State.Input input) {

  /** The description of the option that names the spec file. */
  static final String DESCRIPTION = "The spec file of the service.";

  /** Reads the spec file for the service in the tree. */
  static SpecFile read(Path file, Tree tree) {
    String yaml = State.Input.read(file);
    return new SpecFile(
        SpecReader.parse(yaml), State.Input.of(State.Input.Kind.SPEC, tree.root(), file, yaml));
  }
}
