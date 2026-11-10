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
import me.yakar.tib.state.State;
import me.yakar.tib.state.StateException;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine.Option;

/** The service that a command works on, and the state file that tib keeps in it. */
final class ServiceDirectory {

  @Option(
      names = {"-d", "--dir"},
      paramLabel = "DIR",
      description = "The directory of the service; the current one by default.")
  private Path dir = Path.of(".");

  private final TibVersion version;

  ServiceDirectory(TibVersion version) {
    this.version = version;
  }

  /** The files of the service. */
  Tree tree() {
    return Tree.at(dir);
  }

  /**
   * The state file of the service in the tree, as this tib reads it. A problem of the state file
   * names the service it is in; a directory without one is told how to start a project.
   */
  State state(Tree tree) {
    try {
      return State.of(tree, version.version());
    } catch (StateException e) {
      if (tree.exists(State.PATH)) {
        throw new RefusalException(tree.root() + ": " + e.getMessage());
      }
      throw new RefusalException(e.getMessage(), "Run tib init service first.");
    }
  }

  /** The count with the word that fits it, as in {@code 1 file} or {@code 2 files}. */
  static String count(int count, String one, String many) {
    return count + " " + (count == 1 ? one : many);
  }
}
