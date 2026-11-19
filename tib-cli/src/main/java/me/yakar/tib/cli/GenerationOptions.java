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

import picocli.CommandLine.Option;

/** The options of a command that generates into a service. */
final class GenerationOptions {

  @Option(
      names = "--dry-run",
      description = "Prints what the run would change, as a diff, and changes nothing.")
  private boolean dryRun;

  @Option(
      names = "--allow-dirty",
      description =
          "Writes also when files of the service that git tracks have changes that are not"
              + " committed.")
  private boolean allowDirty;

  /** Whether the run prints its changes as a diff, and writes none. */
  boolean dryRun() {
    return dryRun;
  }

  /** Whether the run writes into a service that has uncommitted changes. */
  boolean allowDirty() {
    return allowDirty;
  }
}
