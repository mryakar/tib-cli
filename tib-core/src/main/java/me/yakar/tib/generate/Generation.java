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
package me.yakar.tib.generate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.TreeMap;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Change;
import me.yakar.tib.tree.Tree;

/**
 * One run of a generator on a project: the generator stages its files in the tree, and then the
 * state file is staged, with every file tib ever wrote to the project.
 */
public final class Generation {

  private Generation() {}

  /**
   * Runs the generator and stages the state file; the tree holds the result until it is applied.
   */
  public static void run(
      Project project, Pack pack, Generator generator, String tibVersion, Tree tree) {
    generator.generate(project, tree);
    TreeMap<String, State.File> files = new TreeMap<>();
    tree.read(State.PATH)
        .map(State::read)
        .ifPresent(previous -> previous.files().forEach(file -> files.put(file.path(), file)));
    for (Change change : tree.changes()) {
      files.put(
          change.path(), new State.File(change.path(), change.owner(), sha256(change.after())));
    }
    State state =
        new State(
            new State.Tib(tibVersion, project.spec().tib()),
            new State.PackRef(pack.name(), pack.version()),
            project.spec(),
            files.values().stream().toList());
    tree.own(State.PATH, state.yaml());
  }

  /** The SHA-256 of a text, in hex. */
  static String sha256(String text) {
    return digest("SHA-256", text);
  }

  static String digest(String algorithm, String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance(algorithm).digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** The state file of the project, if tib ran on it before. */
  public static Optional<State> state(Tree tree) {
    return tree.read(State.PATH).map(State::read);
  }
}
