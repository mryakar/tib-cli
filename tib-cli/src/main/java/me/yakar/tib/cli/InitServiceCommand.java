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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecReader;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** {@code tib init service}. */
@Command(
    name = "service",
    description = "Scaffolds a new Spring Boot service, from its name or its spec file.")
public class InitServiceCommand implements Callable<Integer> {

  @ArgGroup(multiplicity = "1")
  private Input input;

  /** Where the service comes from: its name, or its spec file. */
  static class Input {

    @Parameters(
        arity = "1",
        paramLabel = "NAME",
        description = "The name of the service, in lower case words joined with hyphens.")
    String name;

    @Option(
        names = "--spec",
        required = true,
        paramLabel = "FILE",
        description = "The spec file of the service.")
    Path spec;
  }

  @Mixin private ServiceDirectory directory;

  @Mixin private GenerationOptions options;

  @Override
  public Integer call() {
    Tree tree = directory.tree();
    Spec service;
    List<State.Input> inputs;
    if (input.spec != null) {
      String yaml = State.Input.read(input.spec);
      service = SpecReader.parse(yaml);
      inputs = List.of(State.Input.of(State.Input.Kind.SPEC, tree.root(), input.spec, yaml));
    } else {
      service = new Spec(Spec.VERSION, input.name, null, null, null).check();
      inputs = List.of();
    }
    if (tree.exists(State.PATH)) {
      return options.refuse(
          tree.root() + " is already a tib project: " + State.PATH + " is there.");
    }
    return options.generate(Optional.empty(), service, inputs, Pack::init, tree);
  }
}
