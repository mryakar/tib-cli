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
import me.yakar.tib.generate.Lasting;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.generate.Packs;
import me.yakar.tib.generate.Project;
import me.yakar.tib.git.Git;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Change;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/** The options of a command that generates into a service, and the end of its run. */
final class GenerationOptions {

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
   * of a dry run, or writes the files, says how many, and names the files it deleted. A dry run
   * that changes nothing says so on the standard error. A run that would write into a service with
   * uncommitted changes is refused unless {@code --allow-dirty}. The files that the spec no longer
   * generates and that stay, as they were edited, are named on the standard error, and so are the
   * inputs that the state forgot. The inputs are the files the spec was read from.
   */
  int generate(
      Optional<State> previous,
      Spec service,
      List<State.Input> inputs,
      Function<Pack, Generator> generator,
      Tree tree) {
    Pack pack = Packs.find(service.pack());
    Project project = new Project(service);
    Generator generating = generator.apply(pack);
    Generation.Result result =
        previous.isPresent()
            ? Generation.next(
                previous.get(), project, inputs, pack, generating, version.version(), tree)
            : Generation.first(project, inputs, pack, generating, version.version(), tree);
    PrintWriter out = commandSpec.commandLine().getOut();
    PrintWriter err = commandSpec.commandLine().getErr();
    int files = tree.changes().size();
    if (dryRun) {
      out.print(tree.diff());
      out.flush();
      if (files == 0) {
        err.println(NOTHING_TO_CHANGE);
      }
      notices(err, result);
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
            "Commit or stash them first, so that the changes of tib can be told apart, or run"
                + " again with --allow-dirty.");
      }
      err.println(uncommitted(tree, uncommitted));
      err.println("tib writes anyway, as --allow-dirty asks.");
    }
    List<String> deleted =
        tree.changes().stream().filter(Change::deletes).map(Change::path).toList();
    tree.apply(State.PATH);
    out.println(
        "Wrote "
            + ServiceDirectory.count(files - deleted.size(), "file", "files")
            + " to "
            + tree.root()
            + ".");
    if (!deleted.isEmpty()) {
      out.println(
          "Deleted "
              + ServiceDirectory.count(deleted.size(), "file", "files")
              + " that the spec no longer generates:");
      deleted.forEach(path -> out.println("  " + path));
    }
    notices(err, result);
    return ExitCode.OK;
  }

  /**
   * Names the files that the spec no longer generates and that stay, as they were edited, the
   * inputs that the state forgot, and the files that last and that the spec now generates
   * otherwise, no longer generates, or generates again.
   */
  private static void notices(PrintWriter err, Generation.Result result) {
    if (!result.kept().isEmpty()) {
      err.println(
          "tib no longer generates these files from the spec; they stay, as they were edited:");
      result.kept().forEach(path -> err.println("  " + path));
      err.println("Delete them if the service no longer needs them.");
    }
    if (!result.forgotten().isEmpty()) {
      err.println("tib no longer reads the spec from these files, and forgets them:");
      result.forgotten().forEach(path -> err.println("  " + path));
    }
    if (!result.lastingChanged().isEmpty()) {
      err.println(
          "The spec now generates these files otherwise, but they last, and tib leaves them:");
      result.lastingChanged().forEach(lasting -> err.print(lasting.diff()));
      err.println("Make the change with a file of yours, such as a new migration.");
    }
    if (!result.lastingDropped().isEmpty()) {
      err.println("The spec no longer generates these files, but they last, and tib leaves them:");
      result.lastingDropped().forEach(path -> err.println("  " + path));
      err.println(
          "Undo what they made with a file of yours, such as a migration that drops the table.");
    }
    if (!result.lastingReturned().isEmpty()) {
      err.println(
          "The spec generates these files again, but tib wrote them before, and leaves them:");
      for (Lasting lasting : result.lastingReturned()) {
        if (lasting.diff().isEmpty()) {
          err.println("  " + lasting.path());
        } else {
          err.print(lasting.diff());
        }
      }
      err.println(
          "Make sure the database has what they make, with a file of yours, such as a new"
              + " migration.");
    }
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
