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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Owner;
import me.yakar.tib.tree.Tree;

/**
 * One run of a generator on a project: the generator stages its files in the tree, the files that
 * the spec no longer generates are deleted, and then the state file is staged, with every file the
 * spec was ever read from and the files of tib's that are still there.
 */
public final class Generation {

  private Generation() {}

  /**
   * What a run says beyond its changes.
   *
   * @param kept the paths of the files of the developer's that the spec no longer generates and
   *     that stay, as they were edited, in order
   * @param forgotten the paths of the inputs of the previous state that the new one forgot, in
   *     their order
   */
  public record Result(List<String> kept, List<String> forgotten) {}

  /**
   * Runs the generator that the function picks from the pack, deletes the files that it generated
   * from the previous spec and no longer generates, and stages the state file that {@link
   * State#next} makes of the previous one; the tree holds the result until it is applied. The
   * generator stages no file of the developer's that an earlier run wrote.
   *
   * <p>A file that the spec no longer generates is deleted when the generator owns it, edited or
   * not, and when the developer owns it and it is as tib wrote it. A file of the developer's that
   * was edited stays, and stays on record; so does a file that lasts, and a file that another
   * generator wrote.
   *
   * @param previous the state file of the project, if tib ran on it before
   * @param inputs the files that the spec of this run was read from
   */
  public static Result run(
      Optional<State> previous,
      Project project,
      List<State.Input> inputs,
      Pack pack,
      Function<Pack, Generator> generator,
      String tibVersion,
      Tree tree) {
    previous.ifPresent(state -> tree.writtenBefore(paths(state)));
    Generator generating = generator.apply(pack);
    generating.generate(project, tree);
    List<String> kept = previous.map(state -> drop(state, generating, tree)).orElse(List.of());
    State next =
        State.next(
            previous,
            tibVersion,
            new State.PackRef(pack.name(), pack.version()),
            inputs,
            project.spec(),
            tree);
    tree.own(State.PATH, next.yaml());
    Set<String> read = next.inputs().stream().map(State.Input::path).collect(Collectors.toSet());
    List<String> forgotten =
        previous.stream()
            .flatMap(state -> state.inputs().stream())
            .map(State.Input::path)
            .filter(path -> !read.contains(path))
            .toList();
    return new Result(kept, forgotten);
  }

  /**
   * Deletes the files of the previous state that the run did not generate again: every file of the
   * generator's, and every file of the developer's that the generator writes from the previous spec
   * and that is as tib wrote it, or not there. To tell those from the files that other generators
   * wrote, the generator runs on the previous spec once more, in a tree of its own that is never
   * applied.
   *
   * @return the paths of the files of the developer's that stay, as they were edited
   */
  private static List<String> drop(State previous, Generator generator, Tree tree) {
    Tree before = Tree.at(tree.root());
    before.writtenBefore(paths(previous));
    generator.generate(new Project(previous.spec()), before);
    Set<String> generatedBefore = Set.copyOf(before.generated());
    Set<String> generated = Set.copyOf(tree.generated());
    List<String> kept = new ArrayList<>();
    for (State.File file : previous.files()) {
      String path = file.path();
      if (generated.contains(path)) {
        continue;
      }
      if (file.owner() == Owner.GENERATOR) {
        tree.delete(path, Owner.GENERATOR);
      } else if (generatedBefore.contains(path)) {
        if (tree.read(path).map(content -> asWritten(file, content)).orElse(true)) {
          tree.delete(path, file.owner());
        } else {
          kept.add(path);
        }
      }
    }
    return kept;
  }

  /** Whether the content is that of the file as tib wrote it. */
  private static boolean asWritten(State.File file, String content) {
    return State.File.of(file.path(), file.owner(), content).equals(file);
  }

  private static List<String> paths(State state) {
    return state.files().stream().map(State.File::path).toList();
  }
}
