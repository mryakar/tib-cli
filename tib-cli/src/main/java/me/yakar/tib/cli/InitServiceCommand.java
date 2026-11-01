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

import java.nio.file.Path;
import java.util.concurrent.Callable;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecReader;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/** {@code tib init service}. */
@Command(
    name = "service",
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

  @Mixin private GenerationOptions options;

  @Override
  public Integer call() {
    if (name == null && spec == null) {
      throw new ParameterException(
          commandSpec.commandLine(), "Give the name of the service, or --spec with its spec file.");
    }
    Spec service =
        spec != null
            ? SpecReader.read(spec)
            : new Spec(Spec.VERSION, name, null, null, null).check();
    Tree tree = options.tree();
    if (tree.exists(State.PATH)) {
      return options.refuse(
          tree.root() + " is already a tib project: " + State.PATH + " is there.");
    }
    return options.generate(service, Pack::init, tree);
  }
}
