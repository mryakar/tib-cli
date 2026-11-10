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
import java.util.stream.Stream;
import me.yakar.tib.state.Difference;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

/**
 * {@code tib status}: what tib generated into a service, and which of the files it wrote or read
 * differ.
 */
@Command(
    name = "status",
    description =
        "Says which of the files that tib wrote into the service are no longer as it wrote them,"
            + " and which of the files that it read the spec from were changed since.",
    exitCodeList = {
      "0:Nothing that tib needs differs.",
      "1:A file of tib's was edited or is missing, a file that tib read the spec from was changed,"
          + " or the project has a problem; the message says which.",
      TibCommand.USAGE,
      TibCommand.BUG
    })
public class StatusCommand implements Callable<Integer> {

  @CommandLine.Spec private CommandSpec commandSpec;

  @Mixin private ServiceDirectory directory;

  @Override
  public Integer call() {
    Tree tree = directory.tree();
    State state = directory.state(tree);
    List<Difference> inputs = state.inputDifferences(tree.root());
    List<Difference> files = state.differences(tree);
    PrintWriter out = commandSpec.commandLine().getOut();
    int entities = state.spec().entities().size();
    out.println(
        state.spec().service()
            + ": tib "
            + state.tib()
            + ", pack "
            + state.pack().name()
            + " "
            + state.pack().version()
            + ", "
            + Messages.count(entities, "entity", "entities")
            + ", "
            + Messages.count(state.files().size(), "file", "files")
            + ".");
    if (inputs.isEmpty() && !state.inputs().isEmpty()) {
      out.println("Every file that tib read the spec from is as it read it.");
    }
    if (files.isEmpty()) {
      out.println("Every file of tib's is as it wrote it.");
    }
    list(out, "Read from:", inputs);
    list(out, "Files:", files);
    List<Difference> differences = Stream.concat(inputs.stream(), files.stream()).toList();
    if (differences.isEmpty()) {
      return ExitCode.OK;
    }
    out.println();
    for (Difference.Kind kind : Difference.Kind.values()) {
      if (differences.stream().anyMatch(difference -> difference.kind() == kind)) {
        out.println(explanation(kind));
      }
    }
    boolean wrong = differences.stream().anyMatch(difference -> difference.kind().problem());
    return wrong ? ExitCode.SOFTWARE : ExitCode.OK;
  }

  /** The differences under their heading, after an empty line; nothing when there are none. */
  private static void list(PrintWriter out, String heading, List<Difference> differences) {
    if (differences.isEmpty()) {
      return;
    }
    out.println();
    out.println(heading);
    for (Difference difference : differences) {
      out.printf("%-8s %s%n", difference.kind(), difference.path());
    }
  }

  private static String explanation(Difference.Kind kind) {
    return switch (kind) {
      case CHANGED ->
          "A changed input was edited after tib read the spec from it: the files of tib's follow"
              + " it once tib runs with it again.";
      case ABSENT ->
          "An absent input is no longer where tib read it: tib does not need it, and forgets it"
              + " on its next run.";
      case EDITED ->
          "An edited file is written by tib on every run: git checkout restores it, or the next"
              + " run of tib replaces it, with --allow-dirty while the edit is not committed.";
      case MISSING ->
          "A missing file is one that tib needs: git checkout restores it, or the next run of tib"
              + " writes it again, with --allow-dirty while the deletion is not committed.";
      case REMOVED -> "A removed file was yours: tib does not write it again.";
    };
  }
}
