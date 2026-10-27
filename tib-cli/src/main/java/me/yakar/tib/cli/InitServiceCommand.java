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
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * {@code tib init service}: scaffolds a new Spring Boot service from its name or its spec, with the
 * pack the spec names. With {@code --dry-run} it prints what it would write and writes nothing.
 */
@Component
@Command(
    name = "service",
    mixinStandardHelpOptions = true,
    description = "Scaffolds a new Spring Boot service, from its name or its spec file.")
public class InitServiceCommand implements Callable<Integer> {

  @CommandLine.Spec private CommandSpec commandSpec;

  @Parameters(
      index = "0",
      arity = "0..1",
      paramLabel = "NAME",
      description = "The name of the service, in lower case words joined with hyphens.")
  private String name;

  @Option(
      names = "--spec",
      paramLabel = "FILE",
      description = "The spec file of the service, instead of its name.")
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

  public InitServiceCommand(TibVersion version) {
    this.version = version;
  }

  @Override
  public Integer call() {
    PrintWriter out = commandSpec.commandLine().getOut();
    PrintWriter err = commandSpec.commandLine().getErr();
    if (name == null && spec == null) {
      throw new ParameterException(
          commandSpec.commandLine(), "Give the name of the service, or --spec with its spec file.");
    }
    try {
      Spec service =
          spec != null
              ? SpecReader.read(spec)
              : SpecReader.check(new Spec(Spec.VERSION, name, null, null, null));
      Project project = Project.of(service);
      Pack pack = Packs.find(service.pack());
      Tree tree = Tree.at(dir);
      if (tree.exists(State.PATH)) {
        err.println(tree.root() + " is already a tib project: " + State.PATH + " is there.");
        return 1;
      }
      Generation.run(project, pack, pack.init(), version.version(), tree);
      return Outcome.report(tree, dryRun, out);
    } catch (SpecException | PackException | TreeException | UncheckedIOException e) {
      err.println(e.getMessage());
      return 1;
    }
  }
}
