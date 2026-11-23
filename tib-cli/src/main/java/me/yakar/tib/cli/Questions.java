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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.function.Function;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/**
 * The questions that a command asks for what its command line leaves out: each on standard error,
 * with its answer read from standard input, so that standard output stays the command's own.
 */
// A mixin without options of its own: picocli takes it for one by its @Command.
@Command
final class Questions {

  @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
  private CommandSpec commandSpec;

  private final BufferedReader answers;

  Questions(Reader answers) {
    this.answers = new BufferedReader(answers);
  }

  /**
   * Asks the question until its answer reads, and returns what it reads to. An empty answer is the
   * default; an answer that does not read is reported, and the question asked again.
   *
   * @param defaultAnswer the answer that an empty one stands for, shown after the question; empty
   *     for a question without a default
   * @param read reads an answer, or throws an {@link IllegalArgumentException} that says why it
   *     cannot
   * @throws ParameterException when the answers end before the question has one: the command line
   *     lacks what the question asks for
   * @throws RefusalException when the answers cannot be read
   */
  <T> T ask(String question, String defaultAnswer, Function<String, T> read) {
    PrintWriter err = commandSpec.commandLine().getErr();
    while (true) {
      err.print(question + (defaultAnswer.isEmpty() ? "" : " [" + defaultAnswer + "]") + ": ");
      err.flush();
      String line = line();
      if (line == null) {
        err.println();
        throw new ParameterException(
            commandSpec.commandLine(),
            "The command line leaves out what tib asks for, and there are no more answers.");
      }
      String answer = line.strip();
      try {
        return read.apply(answer.isEmpty() ? defaultAnswer : answer);
      } catch (IllegalArgumentException e) {
        err.println(e.getMessage());
      }
    }
  }

  private String line() {
    try {
      return answers.readLine();
    } catch (IOException e) {
      throw new RefusalException("cannot read the answers: " + e.getMessage());
    }
  }
}
