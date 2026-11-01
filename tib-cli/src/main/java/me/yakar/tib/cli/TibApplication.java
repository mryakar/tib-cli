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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.ParseResult;
import picocli.spring.boot.autoconfigure.PicocliAutoConfiguration;

/**
 * The application that the packages ship. Spring Boot starts, runs the {@code tib} command with
 * picocli, and the process exits with the command's exit code. Only the auto-configuration that the
 * command line needs is imported: the factory of the commands, and the placeholders of the version.
 */
@SpringBootConfiguration
@ComponentScan
@ImportAutoConfiguration({
  PicocliAutoConfiguration.class,
  PropertyPlaceholderAutoConfiguration.class
})
public class TibApplication implements CommandLineRunner, ExitCodeGenerator {

  private final IFactory factory;
  private int exitCode;

  public TibApplication(IFactory factory) {
    this.factory = factory;
  }

  @Override
  public void run(String... args) {
    exitCode = commandLine(factory).execute(args);
  }

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

  @Override
  public int getExitCode() {
    return exitCode;
  }

  /**
   * A run that succeeds ends when this method returns; one that fails ends the process with the
   * exit code of the command.
   */
  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(TibApplication.class);
    // The arguments are tib's, not settings of Spring.
    application.setAddCommandLineProperties(false);
    int exitCode = SpringApplication.exit(application.run(args));
    // Through a stream rather than an if: System.exit never returns, so the coverage tool could
    // not see a plain call to it executed.
    IntStream.of(exitCode).filter(code -> code != 0).forEach(System::exit);
  }
}
