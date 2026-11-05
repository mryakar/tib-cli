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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;

/**
 * One run of a generator on a project: the generator stages its files in the tree, and then the
 * state file is staged, with every file the spec was ever read from and every file tib ever wrote
 * to the project.
 */
public final class Generation {

  private Generation() {}

  /**
   * Runs the generator that the function picks from the pack, and stages the state file that {@link
   * State#next} makes of the previous one; the tree holds the result until it is applied. The
   * generator stages no file of the developer's that an earlier run wrote.
   *
   * @param previous the state file of the project, if tib ran on it before
   * @param inputs the files that the spec of this run was read from
   * @return the paths of the files that the previous state has and the new one forgot, in order:
   *     files of the generator's that the run did not stage again; they stay on disk as they are
   */
  public static List<String> run(
      Optional<State> previous,
      Project project,
      List<State.Input> inputs,
      Pack pack,
      Function<Pack, Generator> generator,
      String tibVersion,
      Tree tree) {
    previous.ifPresent(
        state -> tree.writtenBefore(state.files().stream().map(State.File::path).toList()));
    generator.apply(pack).generate(project, tree);
    State next =
        State.next(
            previous,
            tibVersion,
            new State.PackRef(pack.name(), pack.version()),
            inputs,
            project.spec(),
            tree);
    tree.own(State.PATH, next.yaml());
    Set<String> recorded = next.files().stream().map(State.File::path).collect(Collectors.toSet());
    return previous.stream()
        .flatMap(state -> state.files().stream())
        .map(State.File::path)
        .filter(path -> !recorded.contains(path))
        .toList();
  }
}
