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
class GenerateEntityCommand implements Callable<Integer> {

  @ArgGroup(multiplicity = "1")
  private Source source;

  /** Where the entities come from: a spec file, or DDL. */
  static class Source {

    @Option(
        names = "--spec",
        required = true,
        paramLabel = "FILE",
        description = SpecFile.DESCRIPTION)
    Path spec;

    @Option(
        names = "--ddl",
        required = true,
        paramLabel = "FILE",
        description =
            "SQL DDL whose tables are the entities, added to the spec of the service: its CREATE"
                + " TABLE and ALTER TABLE statements, as in the schema that pg_dump writes; tib"
                + " skips the others.")
    Path ddl;
  }

  @Mixin private ServiceDirectory directory;

  @Mixin private GenerationRun run;

  @Override
  public Integer call() {
    Tree tree = directory.tree();
    State state = directory.state(tree);
    if (source.spec != null) {
      SpecFile file = SpecFile.read(source.spec, tree);
      sameService(file.spec(), state.spec());
      return run.entity(state, file.spec(), file.input(), tree);
    }
    String sql = State.Input.read(source.ddl);
    return run.entity(
        state,
        DdlReader.parse(sql, state.spec()),
        State.Input.of(State.Input.Kind.DDL, tree.root(), source.ddl, sql),
        tree);
  }

  /**
   * Refuses the spec file of another service than the one of the project, of another group, or on
   * another Java.
   */
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
    if (!read.java().equals(project.java())) {
      throw new RefusalException(
          source.spec
              + " is the spec of a service on Java "
              + read.java()
              + "; this project is on Java "
              + project.java(),
          "The Java of a service is chosen when it is created.");
    }
  }
}
