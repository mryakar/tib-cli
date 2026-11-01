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
import java.util.function.Function;
import me.yakar.tib.generate.Generation;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.generate.Packs;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/** The options of a command that generates into a service, and the end of its run. */
public final class GenerationOptions {

  private static final String NOTHING_TO_CHANGE = "Nothing to change.";

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

  private final TibVersion version;

  public GenerationOptions(TibVersion version) {
    this.version = version;
  }

  /** The files of the service. */
  Tree tree() {
    return Tree.at(dir);
  }

  /**
   * Runs the generator that the function picks from the pack of the service, then prints the diff
   * of a dry run, or writes the files and says how many. A dry run that changes nothing says so on
   * the standard error, so that its output stays an empty diff.
   */
  int generate(Spec service, Function<Pack, Generator> generator, Tree tree) {
    Generation.run(
        new Project(service), Packs.find(service.pack()), generator, version.version(), tree);
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
    tree.apply();
    out.println("Wrote " + files + (files == 1 ? " file" : " files") + " to " + tree.root() + ".");
    return ExitCode.OK;
  }

  /** Says why the command does not run. */
  int refuse(String message) {
    commandSpec.commandLine().getErr().println(message);
    return ExitCode.SOFTWARE;
  }
}
