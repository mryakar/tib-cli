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
import java.util.List;
import java.util.concurrent.Callable;
import me.yakar.tib.state.Difference;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

/** {@code tib status}: what tib generated into a service, and which of its files differ. */
@Command(
    name = "status",
    description = {
      "Says which of the files that tib wrote into the service are no longer as it wrote them.",
      "A file of tib's that was edited or is missing is a problem: tib replaces it on its next run."
          + " A file of yours that was removed is not; tib does not write it again."
    },
    exitCodeList = {
      "0:Every file of tib's is as it wrote it.",
      "1:A file of tib's was edited or is missing, or the project has a problem; the message says"
          + " which.",
      "2:The command line is wrong."
    })
public class StatusCommand implements Callable<Integer> {

  @CommandLine.Spec private CommandSpec commandSpec;

  @Mixin private ServiceDirectory directory;

  @Override
  public Integer call() {
    Tree tree = directory.tree();
    State state = directory.state(tree);
    List<Difference> differences = state.differences(tree);
    PrintWriter out = commandSpec.commandLine().getOut();
    int entities = state.spec().entities().size();
    int files = state.files().size();
    out.println(
        state.spec().service()
            + ": tib "
            + state.tib()
            + ", pack "
            + state.pack().name()
            + " "
            + state.pack().version()
            + ", "
            + ServiceDirectory.count(entities, "entity", "entities")
            + ", "
            + ServiceDirectory.count(files, "file", "files")
            + ".");
    if (differences.isEmpty()) {
      out.println("Every file of tib's is as it wrote it.");
      return ExitCode.OK;
    }
    for (Difference difference : differences) {
      out.printf("%-8s %s%n", difference.kind(), difference.path());
    }
    out.println();
    for (Difference.Kind kind : Difference.Kind.values()) {
      if (differences.stream().anyMatch(difference -> difference.kind() == kind)) {
        out.println(explanation(kind));
      }
    }
    boolean wrong =
        differences.stream().anyMatch(difference -> difference.kind() != Difference.Kind.REMOVED);
    return wrong ? ExitCode.SOFTWARE : ExitCode.OK;
  }

  private static String explanation(Difference.Kind kind) {
    return switch (kind) {
      case EDITED -> "An edited file is written by tib on every run: its next run replaces it.";
      case MISSING -> "A missing file is one that tib needs: its next run writes it again.";
      case REMOVED -> "A removed file was yours: tib does not write it again.";
    };
  }
}
