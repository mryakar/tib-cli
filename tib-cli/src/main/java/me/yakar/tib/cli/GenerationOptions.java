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

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import me.yakar.tib.generate.Generation;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.generate.Packs;
import me.yakar.tib.generate.Project;
import me.yakar.tib.git.Git;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/** The options of a command that generates into a service, and the end of its run. */
public final class GenerationOptions {

  private static final String NOTHING_TO_CHANGE = "Nothing to change.";

  /** The uncommitted changes that a message lists; it counts the others. */
  private static final int LISTED = 10;

  @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
  private CommandSpec commandSpec;

  @Option(
      names = {"-d", "--dir"},
      paramLabel = "DIR",
      description = "The directory of the service; the current one by default.")
  private Path dir = Path.of(".");

  @Option(
      names = "--dry-run",
      description = "Print what would be written, as a diff, and write nothing.")
  private boolean dryRun;

  @Option(
      names = "--allow-dirty",
      description =
          "Write also when files of the service that git tracks have changes that are not"
              + " committed.")
  private boolean allowDirty;

  private final TibVersion version;

  GenerationOptions(TibVersion version) {
    this.version = version;
  }

  /** The files of the service. */
  Tree tree() {
    return Tree.at(dir);
  }

  /** The state file of the service, as this tib reads it. */
  State state(Tree tree) {
    return State.of(tree, version.version());
  }

  /**
   * The file that the spec of the service is read from, with its content, as the state keeps it.
   */
  State.Input input(State.Input.Kind kind, Path file, String content) {
    return State.Input.of(kind, dir, file, content);
  }

  /**
   * Runs the generator that the function picks from the pack of the service, then prints the diff
   * of a dry run, or writes the files and says how many. A dry run that changes nothing says so on
   * the standard error, so that its output stays an empty diff. A run that would write into a
   * service whose tracked files have uncommitted changes is refused, so that the changes of tib can
   * be told apart from them, unless the dirty tree is allowed. The previous state is the state file
   * of the service, if there is one; the inputs are the files that the spec of the service was read
   * from.
   */
  int generate(
      Optional<State> previous,
      Spec service,
      List<State.Input> inputs,
      Function<Pack, Generator> generator,
      Tree tree) {
    Generation.run(
        previous,
        new Project(service),
        inputs,
        Packs.find(service.pack()),
        generator,
        version.version(),
        tree);
    PrintWriter out = commandSpec.commandLine().getOut();
    int files = tree.changes().size();
    if (dryRun) {
      out.print(tree.diff());
      out.flush();
      if (files == 0) {
        commandSpec.commandLine().getErr().println(NOTHING_TO_CHANGE);
      }
      return ExitCode.OK;
    }
    if (files == 0) {
      out.println(NOTHING_TO_CHANGE);
      return ExitCode.OK;
    }
    List<String> uncommitted = Git.uncommitted(tree.root());
    if (!uncommitted.isEmpty()) {
      if (!allowDirty) {
        return refuse(
            uncommitted(tree, uncommitted)
                + "Commit or stash them first, so that the changes of tib can be told apart, or run"
                + " again with --allow-dirty.");
      }
      PrintWriter err = commandSpec.commandLine().getErr();
      err.print(uncommitted(tree, uncommitted));
      err.println("tib writes anyway, as --allow-dirty asks.");
    }
    tree.apply();
    out.println("Wrote " + files + (files == 1 ? " file" : " files") + " to " + tree.root() + ".");
    return ExitCode.OK;
  }

  /** The uncommitted changes of the service, a line each, the first ones of many. */
  private static String uncommitted(Tree tree, List<String> changes) {
    StringBuilder message =
        new StringBuilder(
            tree.root() + " has changes that are not committed:" + System.lineSeparator());
    changes.stream()
        .limit(LISTED)
        .forEach(change -> message.append("  ").append(change).append(System.lineSeparator()));
    if (changes.size() > LISTED) {
      message
          .append("  and ")
          .append(changes.size() - LISTED)
          .append(" more")
          .append(System.lineSeparator());
    }
    return message.toString();
  }

  /** Says why the command does not run. */
  int refuse(String message) {
    commandSpec.commandLine().getErr().println(message);
    return ExitCode.SOFTWARE;
  }
}
