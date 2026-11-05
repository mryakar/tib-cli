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
package me.yakar.tib.cli

import java.nio.file.Path
import me.yakar.tib.state.State
import me.yakar.tib.tree.Tree
import picocli.CommandLine

/** Runs the tib command in this JVM, as the application does, with a version of the test's own. */
class Tib {

  /** The examples of the documentation: the spec and the DDL of a service. */
  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  /** What a run printed, with the line separator of every platform, and its exit code. */
  record Result(int exitCode, String out, String err) {}

  /** The state file of the service, as a tib newer than those of the tests reads it. */
  static State state(Path service) {
    State.of(Tree.at(service), '9.9.9')
  }

  private final String version

  Tib(String version = '1.2.3') {
    this.version = version
  }

  Result run(String... args) {
    StringWriter out = new StringWriter()
    StringWriter err = new StringWriter()
    int exitCode = TibApplication.commandLine(new CommandFactory(new TibVersion(version)))
        .setOut(new PrintWriter(out))
        .setErr(new PrintWriter(err))
        // Without colors: whether picocli would use them depends on the platform and the console.
        .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.OFF))
        .execute(args)
    new Result(exitCode, out.toString().replace('\r\n', '\n'), err.toString().replace('\r\n', '\n'))
  }
}
