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

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.state.State
import me.yakar.tib.tree.Tree
import picocli.CommandLine

/** Runs the tib command in this JVM, as the application does, with a version of the test's own. */
class Tib {

  /** The examples of the documentation: the spec and the DDL of a service. */
  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  /** The spec file of the documentation: a vehicle and a depot. */
  static final String SPEC = EXAMPLES.resolve('vehicle-service.yaml')

  /** The DDL of the documentation: the tables of the vehicle and of the depot. */
  static final String DDL = EXAMPLES.resolve('vehicle-service.sql')

  /** The spec of the documentation without its vehicle: the depot only. */
  static final String DEPOT_ONLY = Files.readString(Path.of(SPEC))
  .replaceFirst(/(?s)  - name: Vehicle.*(?=  - name: Depot)/, '')

  /** What a run printed, with the line separator of every platform, and its exit code. */
  record Result(int exitCode, String out, String err) {}

  /** The directory of the service that the commands of the test work on, if any. */
  final Path service

  /** The directory of the service as the messages of tib name it. */
  final String root

  private final String version

  Tib(Path service = null, String version = '1.2.3') {
    this.service = service
    this.root = service?.toAbsolutePath()?.normalize()?.toString()
    this.version = version
  }

  /** Runs {@code tib init service} on the service. */
  Result init(String... args) {
    run('init', 'service', *args, '-d', service.toString())
  }

  /** Runs {@code tib generate entity} on the service. */
  Result generate(String... args) {
    run('generate', 'entity', *args, '-d', service.toString())
  }

  /** Runs {@code tib status} on the service. */
  Result status() {
    run('status', '-d', service.toString())
  }

  /** Writes the file into the service; its path. */
  String write(String name, String content) {
    Files.writeString(service.resolve(name), content).toString()
  }

  /** The state file of the service, as a tib newer than those of the tests reads it. */
  State state() {
    State.of(Tree.at(service), '9.9.9')
  }

  /**
   * Every file and directory of the service by path, with the content of each file, a character for
   * each byte; git's own files are not the service's.
   */
  Map<String, String> files() {
    Files.walk(service).withCloseable { paths ->
      paths.filter {
        it != service && !service.relativize(it).startsWith('.git')
      }.toList().collectEntries {
        [
          service.relativize(it).toString(),
          Files.isDirectory(it) ? 'a directory' : new String(Files.readAllBytes(it), 'ISO-8859-1')
        ]
      }
    }
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
