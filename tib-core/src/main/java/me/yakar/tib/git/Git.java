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
package me.yakar.tib.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** What git says about the directory of a project, asked through the git command of the machine. */
public final class Git {

  private Git() {}

  /**
   * The files under the directory that git tracks and whose changes are not committed, staged or
   * not, as {@code git status --porcelain} prints them: {@code " M path"}, the path from the root
   * of the repository. None when the directory is not in a repository, or there is no git to ask.
   */
  public static List<String> uncommitted(Path directory) {
    return uncommitted(directory, "git");
  }

  static List<String> uncommitted(Path directory, String git) {
    ProcessBuilder status =
        new ProcessBuilder(
                git,
                "-c",
                "core.quotePath=false",
                "-C",
                directory.toString(),
                "status",
                "--porcelain",
                "--untracked-files=no",
                "--",
                ".")
            .redirectError(ProcessBuilder.Redirect.DISCARD);
    // git status may refresh the index, which takes its lock; reading needs no lock.
    status.environment().put("GIT_OPTIONAL_LOCKS", "0");
    try {
      Process process = status.start();
      List<String> changes;
      try (BufferedReader output = process.inputReader(StandardCharsets.UTF_8)) {
        changes = output.lines().toList();
      }
      // onExit, not waitFor: an interrupted thread still gets git's answer.
      return process.onExit().join().exitValue() == 0 ? changes : List.of();
    } catch (IOException e) {
      return List.of();
    }
  }
}
