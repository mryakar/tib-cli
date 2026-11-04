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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Change;
import me.yakar.tib.tree.Tree;

/**
 * One run of a generator on a project: the generator stages its files in the tree, and then the
 * state file is staged, with every file the spec was ever read from and every file tib ever wrote
 * to the project.
 */
public final class Generation {

  private Generation() {}

  /**
   * Runs the generator that the function picks from the pack, and stages the state file; the tree
   * holds the result until it is applied. The generator stages no file of the developer's that an
   * earlier run wrote. The inputs are the files that the spec of this run was read from; each takes
   * the place of the input of the same path in the state, and the others are added after them.
   */
  public static void run(
      Project project,
      List<State.Input> inputs,
      Pack pack,
      Function<Pack, Generator> generator,
      String tibVersion,
      Tree tree) {
    LinkedHashMap<String, State.Input> read = new LinkedHashMap<>();
    TreeMap<String, State.File> files = new TreeMap<>();
    State.in(tree)
        .ifPresent(
            previous -> {
              previous.inputs().forEach(input -> read.put(input.path(), input));
              previous.files().forEach(file -> files.put(file.path(), file));
            });
    inputs.forEach(input -> read.put(input.path(), input));
    tree.writtenBefore(files.keySet());
    generator.apply(pack).generate(project, tree);
    for (Change change : tree.changes()) {
      files.put(change.path(), State.File.of(change.path(), change.owner(), change.after()));
    }
    State state =
        new State(
            tibVersion,
            new State.PackRef(pack.name(), pack.version()),
            read.values().stream().toList(),
            project.spec(),
            files.values().stream().toList());
    tree.own(State.PATH, state.yaml());
  }
}
