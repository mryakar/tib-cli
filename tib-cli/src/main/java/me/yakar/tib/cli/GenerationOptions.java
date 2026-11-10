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
import me.yakar.tib.generate.Generation;
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

/** The options of a command that generates into a service, and the run that they steer. */
final class GenerationOptions {

  /** The description of the option that names the spec file. */
  static final String SPEC_FILE = "The spec file of the service.";

  private static final String NOTHING_TO_CHANGE = "Nothing to change.";

  /** The uncommitted changes that a message lists; it counts the others. */
  private static final int LISTED = 10;

  @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
  private CommandSpec commandSpec;

  @Option(
      names = "--dry-run",
      description = "Prints what the run would change, as a diff, and changes nothing.")
  private boolean dryRun;

  @Option(
      names = "--allow-dirty",
      description =
          "Writes also when files of the service that git tracks have changes that are not"
              + " committed.")
  private boolean allowDirty;

  private final TibVersion version;

  GenerationOptions(TibVersion version) {
    this.version = version;
  }

  /**
   * Scaffolds the service into a directory that tib did not run on, and ends the run as {@link
   * #end} does.
   *
   * @param inputs the files that the spec was read from
   */
  int init(Spec service, List<State.Input> inputs, Tree tree) {
    Pack pack = Packs.find(service.pack());
    return end(
        Generation.first(new Project(service), inputs, pack, pack.init(), version.version(), tree),
        tree);
  }

  /**
   * Generates the entity layer into the service that has the state, and ends the run as {@link
   * #end} does.
   *
   * @param input the file that the spec was read from
   */
  int entity(State previous, Spec service, State.Input input, Tree tree) {
    Pack pack = Packs.find(service.pack());
    return end(
        Generation.next(
            previous,
            new Project(service),
            List.of(input),
            pack,
            pack.entity(),
            version.version(),
            tree),
        tree);
  }

  /**
   * Ends a run that staged its changes in the tree: a dry run prints the diff; otherwise the
   * changes are written, unless the service has uncommitted changes and {@code --allow-dirty} is
   * not given. A run without changes says so, a dry run on the standard error.
   */
  private int end(Generation.Result result, Tree tree) {
    PrintWriter out = commandSpec.commandLine().getOut();
    PrintWriter err = commandSpec.commandLine().getErr();
    List<Change> changes = tree.changes();
    if (dryRun) {
      out.print(tree.diff());
      out.flush();
      if (changes.isEmpty()) {
        err.println(NOTHING_TO_CHANGE);
      }
    } else if (changes.isEmpty()) {
      out.println(NOTHING_TO_CHANGE);
    } else {
      write(changes, tree, out, err);
    }
    notices(err, result);
    return ExitCode.OK;
  }

  /** Writes the changes, and says how many files were written and which were deleted. */
  private void write(List<Change> changes, Tree tree, PrintWriter out, PrintWriter err) {
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
    List<String> deleted = changes.stream().filter(Change::deletes).map(Change::path).toList();
    tree.apply(State.PATH);
    out.println("Wrote " + files(changes.size() - deleted.size()) + " to " + tree.root() + ".");
    if (!deleted.isEmpty()) {
      out.println("Deleted " + files(deleted.size()) + " that the spec no longer generates:");
      deleted.forEach(path -> out.println("  " + path));
    }
  }

  private static String files(int count) {
    return Messages.count(count, "file", "files");
  }

  /** Says on the standard error what stays or is forgotten although the spec changed. */
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
    return String.join("\n", lines);
  }
}
