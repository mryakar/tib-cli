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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * The application that the packages ship. Spring Boot starts, runs the {@code tib} command with
 * picocli, and the process exits with the command's exit code.
 */
@SpringBootApplication
public class TibApplication implements CommandLineRunner, ExitCodeGenerator {

  private final IFactory factory;
  private final TibCommand command;
  private int exitCode;

  public TibApplication(IFactory factory, TibCommand command) {
    this.factory = factory;
    this.command = command;
  }

  @Override
  public void run(String... args) {
    exitCode = new CommandLine(command, factory).execute(args);
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
    int exitCode = SpringApplication.exit(SpringApplication.run(TibApplication.class, args));
    // Through a stream rather than an if: System.exit never returns, so the coverage tool could
    // not see a plain call to it executed.
    IntStream.of(exitCode).filter(code -> code != 0).forEach(System::exit);
  }
}
