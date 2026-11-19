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

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ScopeType;

/** {@code tib}. */
@Command(
    name = "tib",
    mixinStandardHelpOptions = true,
    versionProvider = TibVersion.class,
    scope = ScopeType.INHERIT,
    description = "The Idempotent Boilerplate: a scaffolding CLI for Spring Boot services.",
    exitCodeListHeading = "Exit codes:%n",
    exitCodeList = {
      "0:Done, or nothing to change.",
      TibApplication.PROBLEM
          + ":The spec, the DDL, the pack or the project has a problem; the message says which.",
      TibCommand.USAGE_MEANS,
      TibCommand.BUG_MEANS
    },
    subcommands = {
      InitCommand.class,
      GenerateCommand.class,
      StatusCommand.class,
      SchemaCommand.class
    })
public class TibCommand {

  /** What exit code 2 means, for every command. */
  static final String USAGE_MEANS = ExitCode.USAGE + ":The command line is wrong.";

  /** What exit code 3 means, for every command. */
  static final String BUG_MEANS =
      TibApplication.BUG + ":tib failed; a bug, reported with its stack trace.";
}
