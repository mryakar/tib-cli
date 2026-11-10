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
import me.yakar.tib.ddl.DdlReader;
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
  private Source source;

  /** Where the entities come from: a spec file, or DDL. */
  static class Source {

    @Option(
        names = "--spec",
        required = true,
        paramLabel = "FILE",
        description = GenerationOptions.SPEC_FILE)
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

  @Mixin private ServiceDirectory directory;

  @Mixin private GenerationOptions options;

  @Override
  public Integer call() {
    Tree tree = directory.tree();
    State state = directory.state(tree);
    Path file = source.spec != null ? source.spec : source.ddl;
    String content = State.Input.read(file);
    State.Input.Kind kind;
    Spec service;
    if (source.spec != null) {
      kind = State.Input.Kind.SPEC;
      service = SpecReader.parse(content);
      sameService(service, state.spec());
    } else {
      kind = State.Input.Kind.DDL;
      service = DdlReader.parse(content, state.spec());
    }
    return options.entity(state, service, State.Input.of(kind, tree.root(), file, content), tree);
  }

  /** Refuses the spec file of another service than the one of the project, or of another group. */
  private void sameService(Spec read, Spec project) {
    if (!read.service().equals(project.service())) {
      throw new RefusalException(
          source.spec
              + " is the spec of "
              + read.service()
              + "; this project is "
              + project.service());
    }
    if (!read.group().equals(project.group())) {
      throw new RefusalException(
          source.spec
              + " is the spec of a service in "
              + read.group()
              + "; this project is in "
              + project.group());
    }
  }
}
