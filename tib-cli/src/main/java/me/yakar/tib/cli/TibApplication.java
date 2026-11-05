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

import java.util.stream.IntStream;
import me.yakar.tib.TibException;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.ParseResult;

/** The application that the packages ship: runs the {@code tib} command and exits with its code. */
public final class TibApplication {

  private TibApplication() {}

  /** The {@code tib} command, with its subcommands built by the factory. */
  static CommandLine commandLine(IFactory factory) {
    return new CommandLine(TibCommand.class, factory)
        .setExecutionExceptionHandler(TibApplication::report);
  }

  /**
   * Prints the message of a failure that the user can fix, exit code 1; any other exception is a
   * bug, which picocli reports with its stack trace.
   */
  static int report(Exception e, CommandLine commandLine, ParseResult parseResult)
      throws Exception {
    if (e instanceof TibException) {
      commandLine.getErr().println(e.getMessage());
      return ExitCode.SOFTWARE;
    }
    throw e;
  }

  /** Runs tib; a run that fails ends the process with the exit code of the command. */
  public static void main(String[] args) {
    int exitCode = commandLine(new CommandFactory()).execute(args);
    // Not an if: coverage cannot count a System.exit call, which never returns.
    IntStream.of(exitCode).filter(code -> code != 0).forEach(System::exit);
  }
}
