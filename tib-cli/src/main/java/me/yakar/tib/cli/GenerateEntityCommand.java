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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import me.yakar.tib.generate.Generation;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.generate.PackException;
import me.yakar.tib.generate.Packs;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecException;
import me.yakar.tib.spec.SpecReader;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import me.yakar.tib.tree.TreeException;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/**
 * {@code tib generate entity}: generates the entity layer of every entity of the spec into the
 * service that tib scaffolded. A run that would change nothing, changes nothing.
 */
@Component
@Command(
    name = "entity",
    mixinStandardHelpOptions = true,
    description = "Generates the entity layer of the entities of the spec.")
public class GenerateEntityCommand implements Callable<Integer> {

  @CommandLine.Spec private CommandSpec commandSpec;

  @Option(
      names = "--spec",
      required = true,
      paramLabel = "FILE",
      description = "The spec file of the service.")
  private Path spec;

  @Option(
      names = {"-d", "--dir"},
      paramLabel = "DIR",
      description = "The directory of the service; the current one by default.")
  private Path dir = Path.of(".");

  @Option(
      names = "--dry-run",
      description = "Prints what would be written, as a diff, and writes nothing.")
  private boolean dryRun;

  private final TibVersion version;

  public GenerateEntityCommand(TibVersion version) {
    this.version = version;
  }

  @Override
  public Integer call() {
    PrintWriter out = commandSpec.commandLine().getOut();
    PrintWriter err = commandSpec.commandLine().getErr();
    try {
      Spec service = SpecReader.read(spec);
      Project project = Project.of(service);
      Pack pack = Packs.find(service.pack());
      Tree tree = Tree.at(dir);
      if (!tree.exists(State.PATH)) {
        err.println(
            tree.root()
                + " is not a tib project: there is no "
                + State.PATH
                + ". Run tib init service first.");
        return 1;
      }
      Generation.run(project, pack, pack.entity(), version.version(), tree);
      return Outcome.report(tree, dryRun, out);
    } catch (SpecException | PackException | TreeException | UncheckedIOException e) {
      err.println(e.getMessage());
      return 1;
    }
  }
}
