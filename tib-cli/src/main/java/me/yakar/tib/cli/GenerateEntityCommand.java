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
import me.yakar.tib.ddl.DdlReader;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecReader;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/** {@code tib generate entity}. */
@Command(
    name = "entity",
    description =
        "Generates the entity layer of the entities of the spec, or of the tables of DDL.")
public class GenerateEntityCommand implements Callable<Integer> {

  @ArgGroup(multiplicity = "1")
  private Input input;

  /** Where the entities come from: a spec file, or DDL. */
  static class Input {

    @Option(
        names = "--spec",
        required = true,
        paramLabel = "FILE",
        description = "The spec file of the service.")
    Path spec;

    @Option(
        names = "--ddl",
        required = true,
        paramLabel = "FILE",
        description =
            "SQL DDL whose tables are the entities, added to the spec of the service: CREATE TABLE"
                + " statements, and ALTER TABLE statements that add keys.")
    Path ddl;
  }

  @Mixin private GenerationOptions options;

  @Override
  public Integer call() {
    Tree tree = options.tree();
    State state = options.state(tree);
    Spec project = state.spec();
    if (input.spec == null) {
      String ddl = State.Input.read(input.ddl);
      return options.generate(
          Optional.of(state),
          DdlReader.parse(ddl, project),
          List.of(options.input(State.Input.Kind.DDL, input.ddl, ddl)),
          Pack::entity,
          tree);
    }
    String yaml = State.Input.read(input.spec);
    Spec service = SpecReader.parse(yaml);
    if (!service.service().equals(project.service())) {
      return options.refuse(
          input.spec
              + " is the spec of "
              + service.service()
              + "; this project is "
              + project.service());
    }
    if (!service.group().equals(project.group())) {
      return options.refuse(
          input.spec
              + " is the spec of a service in "
              + service.group()
              + "; this project is in "
              + project.group());
    }
    return options.generate(
        Optional.of(state),
        service,
        List.of(options.input(State.Input.Kind.SPEC, input.spec, yaml)),
        Pack::entity,
        tree);
  }
}
