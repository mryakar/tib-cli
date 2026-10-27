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
import picocli.CommandLine
import spock.lang.Specification
import spock.lang.TempDir

class InitServiceCommandSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  @TempDir
  Path dir

  StringWriter out = new StringWriter()
  StringWriter err = new StringWriter()
  CommandLine tib = new CommandLine(new TibCommand(), new TibCommandSpec.TestFactory())
  .setOut(new PrintWriter(out))
  .setErr(new PrintWriter(err))
  .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.OFF))

  def "tib init alone prints its usage"() {
    expect:
    tib.execute('init') == 0
    output().startsWith('Usage: tib init')
  }

  def "without a name or a spec it asks for one"() {
    expect:
    tib.execute('init', 'service') == 2
    error().startsWith('Give the name of the service, or --spec with its spec file.')
  }

  def "a dry run prints the diff and writes nothing"() {
    when:
    int exitCode = tib.execute('init', 'service', 'vehicle-service', '--dir', dir.toString(), '--dry-run')

    then:
    exitCode == 0
    output().startsWith('--- /dev/null\n+++ b/.gitignore\n')
    output().contains('+++ b/src/main/java/com/example/vehicleservice/VehicleServiceApplication.java\n')
    Files.list(dir).count() == 0
  }

  def "scaffolds the service and records what it wrote"() {
    when:
    int exitCode = tib.execute('init', 'service', 'vehicle-service', '-d', dir.toString())

    then:
    exitCode == 0
    output() == "Wrote 9 files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/java/com/example/vehicleservice/VehicleServiceApplication.java'))
    State state = State.read(Files.readString(dir.resolve(State.PATH)))
    state.tib().version() == '1.2.3'
    state.pack().name() == 'default'
    state.spec().service() == 'vehicle-service'
    state.files().size() == 8

    when: 'again'
    int again = tib.execute('init', 'service', 'vehicle-service', '-d', dir.toString())

    then:
    again == 1
    error() == "${dir.toAbsolutePath().normalize()} is already a tib project: .tib/state.yaml is there.\n"
  }

  def "scaffolds the service of a spec file"() {
    when:
    int exitCode = tib.execute('init', 'service', '--spec', EXAMPLES.resolve('vehicle-service.yaml').toString(), '-d', dir.toString())

    then:
    exitCode == 0
    State.read(Files.readString(dir.resolve(State.PATH))).spec().entities()*.name() == ['Vehicle', 'Depot']
  }

  def "a problem of the spec is reported: #args"() {
    expect:
    tib.execute(*(['init', 'service'] + args + ['-d', dir.toString()])) == 1
    error() == message

    where:
    args                               | message
    ['Vehicle Service']                | "the spec has a problem: service: 'Vehicle Service' does not match ^[a-z][a-z0-9]*(-[a-z0-9]+)*\$\n"
    ['--spec', spec('pack: nope')]     | "there is no pack 'nope'\n"
    ['--spec', spec('tib: 2')]         | "the spec has a problem: tib: this build reads version 1\n"
  }

  def "a directory that cannot be written to is reported"() {
    given:
    Path file = Files.writeString(dir.resolve('file'), '')

    expect:
    tib.execute('init', 'service', 's', '-d', file.toString()) == 1
    error().contains('file')
  }

  /** What the command printed, with the line separator of every platform. */
  private String output() {
    out.toString().replace('\r\n', '\n')
  }

  private String error() {
    err.toString().replace('\r\n', '\n')
  }

  private static String spec(String line) {
    Path file = Files.createTempFile('spec', '.yaml')
    Files.writeString(file, "tib: 1\nservice: s\n$line\n".replace('tib: 1\nservice: s\ntib: 2', 'tib: 2\nservice: s'))
    file.toString()
  }
}
