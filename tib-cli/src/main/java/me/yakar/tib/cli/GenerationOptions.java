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
import java.util.ArrayList;
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

  /**
   * Runs the generator that the function picks from the pack of the service, then prints the diff
   * of a dry run, or writes the files and says how many. A dry run that changes nothing says so on
   * the standard error. A run that would write into a service with uncommitted changes is refused
   * unless {@code --allow-dirty}. The inputs are the files the spec was read from.
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
        throw new RefusalException(
            uncommitted(tree, uncommitted),
            uncommitted,
            "Commit or stash them first, so that the changes of tib can be told apart, or run"
                + " again with --allow-dirty.");
      }
      PrintWriter err = commandSpec.commandLine().getErr();
      err.println(uncommitted(tree, uncommitted));
      err.println("tib writes anyway, as --allow-dirty asks.");
    }
    tree.apply();
    out.println(
        "Wrote " + ServiceDirectory.count(files, "file", "files") + " to " + tree.root() + ".");
    return ExitCode.OK;
  }

  /** The uncommitted changes of the service, a line each, the first ones of many. */
  private static String uncommitted(Tree tree, List<String> changes) {
    List<String> lines = new ArrayList<>();
    lines.add(tree.root() + " has changes that are not committed:");
    changes.stream().limit(LISTED).forEach(change -> lines.add("  " + change));
    if (changes.size() > LISTED) {
      lines.add("  and " + (changes.size() - LISTED) + " more");
    }
    return String.join(System.lineSeparator(), lines);
  }
}
