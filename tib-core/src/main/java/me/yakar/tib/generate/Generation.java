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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Change;
import me.yakar.tib.tree.Owner;
import me.yakar.tib.tree.Tree;

/**
 * One run of a generator on a project: the generator stages its files in the tree, a later run
 * deletes the files that the spec no longer generates, and then the state file is staged, with the
 * files that the spec is read from and the files of tib's that are there.
 */
public final class Generation {

  private Generation() {}

  /**
   * What a run says beyond its changes.
   *
   * @param kept the files of the developer's that the spec no longer generates and that stay, as
   *     they were edited; in the order of their paths
   * @param forgotten the inputs of the previous state that the new one forgot; in the order of the
   *     previous state
   * @param lastingChanged the files that last, that the spec now generates otherwise, and that tib
   *     does not write: from what the previous spec generated to what this one generates; in the
   *     order of their paths
   * @param lastingDropped the files that last, that the spec no longer generates, and that are on
   *     record or there; in the order of their paths
   * @param lastingReturned the files that last, that the previous spec did not generate and this
   *     one does, and that tib does not write, as it wrote them before or they are there: from the
   *     file as it is in the project to what the spec generates; in the order of their paths
   */
  public record Result(
      List<String> kept,
      List<String> forgotten,
      List<Lasting> lastingChanged,
      List<String> lastingDropped,
      List<Lasting> lastingReturned) {}

  /**
   * The first run on a project: runs the generator and stages the state file; the tree holds the
   * result until it is applied.
   *
   * @param inputs the files that the spec of this run was read from
   * @param pack the pack of the generator
   */
  public static Result first(
      Project project,
      List<State.Input> inputs,
      Pack pack,
      Generator generator,
      String tibVersion,
      Tree tree) {
    Recorder recorder = new Recorder(tree);
    generator.generate(project, recorder);
    State first =
        new State(tibVersion, ref(pack), inputs, project.spec(), List.copyOf(recorder.written()));
    tree.own(State.PATH, first.yaml());
    return new Result(List.of(), List.of(), List.of(), List.of(), List.of());
  }

  /**
   * A later run on a project: runs the generator, drops the files that it generated from the
   * previous spec and no longer generates, and stages the state file that {@link State#next} makes
   * of the previous one; the tree holds the result until it is applied. The generator stages no
   * file of the developer's that an earlier run wrote.
   *
   * <p>A file that the spec no longer generates is deleted when the generator owns it, edited or
   * not, whichever generator wrote it. A file of the developer's that this generator wrote once
   * from the previous spec is deleted when it is as tib wrote it; when it was edited it stays and
   * leaves the record: it is the developer's alone, and tib writes it again only when the spec
   * generates it again and it is not there. A file that lasts stays, on record, and so does a file
   * of the developer's that another generator wrote.
   *
   * <p>A service that was generated for another stack than the one of the pack, or before tib
   * recorded the stack, is refused before anything is staged: the files that tib wrote once, such
   * as the build, are of that stack.
   *
   * @param previous the state file of the project
   * @param inputs the files that the spec of this run was read from
   * @param pack the pack of the generator
   * @throws PackException when the pack generates for another stack than the service has
   */
  public static Result next(
      State previous,
      Project project,
      List<State.Input> inputs,
      Pack pack,
      Generator generator,
      String tibVersion,
      Tree tree) {
    if (!pack.stack().equals(previous.pack().stack())) {
      throw new PackException(
          tree.root()
              + " was generated for "
              + Objects.requireNonNullElse(previous.pack().stack(), "an earlier stack")
              + " by tib "
              + previous.tib()
              + "; pack "
              + pack.name()
              + " "
              + pack.version()
              + " generates for "
              + pack.stack()
              + "\nGo on with tib "
              + previous.tib()
              + " for this service, or move it to "
              + pack.stack()
              + " by hand.");
    }
    Recorder now = generate(generator, project, previous, tree);
    // What the generator makes of the previous spec, in a tree of its own that is never applied.
    Recorder before =
        generate(generator, new Project(previous.spec()), previous, Tree.at(tree.root()));
    Dropped dropped = drop(previous, before.generated(), now.generated(), tree);
    State next =
        previous.next(
            tibVersion,
            ref(pack),
            inputs,
            project.spec(),
            tree.root(),
            now.written(),
            Stream.concat(dropped.deleted().stream(), dropped.kept().stream()).toList());
    tree.own(State.PATH, next.yaml());
    Left left = left(previous, before.lasting(), now.lasting(), tree);
    return new Result(
        dropped.kept(),
        previous.inputs().stream()
            .map(State.Input::path)
            .filter(path -> next.inputs().stream().noneMatch(input -> input.path().equals(path)))
            .toList(),
        left.changed(),
        left.dropped(),
        left.returned());
  }

  /** The files that last and that the run leaves as they are, as {@link Result} names them. */
  private record Left(List<Lasting> changed, List<String> dropped, List<Lasting> returned) {}

  /**
   * The files that last and that the run leaves as they are, where the spec says otherwise. A file
   * that the spec no longer generates is named when it is on record or there. A file that the spec
   * generates and that the run does not write is named when the previous spec did not generate it,
   * or generated it otherwise.
   *
   * @param before the files that the generator makes of the previous spec, with their content
   * @param now the files that it makes of this one
   */
  private static Left left(
      State previous, SortedMap<String, byte[]> before, SortedMap<String, byte[]> now, Tree tree) {
    Set<String> recorded =
        previous.files().stream().map(State.File::path).collect(Collectors.toSet());
    Set<String> staged = tree.changes().stream().map(Change::path).collect(Collectors.toSet());
    List<Lasting> changed = new ArrayList<>();
    List<String> dropped = new ArrayList<>();
    List<Lasting> returned = new ArrayList<>();
    SortedSet<String> paths = new TreeSet<>(before.keySet());
    paths.addAll(now.keySet());
    for (String path : paths) {
      byte[] made = before.get(path);
      byte[] makes = now.get(path);
      if (makes == null) {
        if (recorded.contains(path) || tree.exists(path)) {
          dropped.add(path);
        }
      } else if (!staged.contains(path)) {
        if (made == null) {
          returned.add(new Lasting(path, tree.bytes(path).orElse(null), makes));
        } else if (!Arrays.equals(made, makes)) {
          changed.add(new Lasting(path, made, makes));
        }
      }
    }
    return new Left(List.copyOf(changed), List.copyOf(dropped), List.copyOf(returned));
  }

  private static State.PackRef ref(Pack pack) {
    return new State.PackRef(pack.name(), pack.version(), pack.stack());
  }

  /**
   * Runs the generator on the tree, which is told the files that tib wrote before, and records what
   * it generates.
   */
  private static Recorder generate(
      Generator generator, Project project, State previous, Tree tree) {
    tree.writtenBefore(previous.files().stream().map(State.File::path).toList());
    Recorder recorder = new Recorder(tree);
    generator.generate(project, recorder);
    return recorder;
  }

  /**
   * The files of the previous state that the run did not generate again.
   *
   * @param deleted the paths of the files whose deletion is staged, or that are not there
   * @param kept the paths of the files of the developer's that stay, as they were edited
   */
  private record Dropped(List<String> deleted, List<String> kept) {}

  /**
   * Drops the files of the previous state that the run did not generate again, as {@link #next}
   * says.
   *
   * @param generatedBefore the paths that the generator generates from the previous spec
   * @param generated the paths that it generates from this one
   */
  private static Dropped drop(
      State previous, Set<String> generatedBefore, Set<String> generated, Tree tree) {
    List<String> deleted = new ArrayList<>();
    List<String> kept = new ArrayList<>();
    for (State.File file : previous.files()) {
      String path = file.path();
      if (generated.contains(path)) {
        continue;
      }
      if (file.owner() == Owner.GENERATOR
          || generatedBefore.contains(path) && tree.bytes(path).map(file::holds).orElse(true)) {
        tree.delete(path, file.owner());
        deleted.add(path);
      } else if (generatedBefore.contains(path)) {
        kept.add(path);
      }
    }
    return new Dropped(deleted, kept);
  }
}
