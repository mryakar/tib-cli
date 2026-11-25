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
import java.util.Set;
import java.util.concurrent.Callable;
import me.yakar.tib.ddl.DdlReader;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.TestKind;
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
   * Refuses a spec file that is not the one of this service: what is chosen when a service is
   * created does not change.
   */
  private void sameService(Spec read, Spec project) {
    same(read.service().equals(project.service()), read.service(), "is " + project.service(), null);
    same(
        read.group().equals(project.group()),
        "a service in " + read.group(),
        "is in " + project.group(),
        null);
    same(
        read.java().equals(project.java()),
        "a service on Java " + read.java(),
        "is on Java " + project.java(),
        "The Java of a service is chosen when it is created.");
    same(
        read.architecture() == project.architecture(),
        "a service of the architecture " + read.architecture(),
        "is " + project.architecture(),
        "The architecture of a service is chosen when it is created.");
    same(
        Set.copyOf(read.tests()).equals(Set.copyOf(project.tests())),
        "a service with " + kinds(read.tests()),
        "has " + kinds(project.tests()),
        "The kinds of tests of a service are chosen when it is created.");
    same(
        read.docker().equals(project.docker()),
        "a service " + (read.docker() ? "with" : "without") + " a Dockerfile",
        "has " + (project.docker() ? "one" : "none"),
        "Whether a service has a Dockerfile is chosen when it is created.");
  }

  /**
   * Refuses the spec file unless it says the same as the project.
   *
   * @param read what the file is the spec of, as in {@code a service on Java 25}
   * @param project what the project is or has instead, as in {@code is on Java 21}
   * @param advice what follows the problem on a line of its own, or null for nothing
   */
  private void same(boolean same, String read, String project, String advice) {
    if (same) {
      return;
    }
    String problem = source.spec + " is the spec of " + read + "; this project " + project;
    throw advice == null ? new RefusalException(problem) : new RefusalException(problem, advice);
  }

  /** The kinds of tests in words, as in {@code unit and api tests}, or {@code no tests}. */
  private static String kinds(List<TestKind> tests) {
    if (tests.isEmpty()) {
      return "no tests";
    }
    List<String> names = tests.stream().map(TestKind::toString).toList();
    int last = names.size() - 1;
    return (last == 0
            ? names.get(0)
            : String.join(", ", names.subList(0, last)) + " and " + names.get(last))
        + " tests";
  }
}
