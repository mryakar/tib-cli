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

class GenerateEntityCommandSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  @TempDir
  Path dir

  StringWriter out = new StringWriter()
  StringWriter err = new StringWriter()
  CommandLine tib = new CommandLine(new TibCommand(), new TibCommandSpec.TestFactory())
  .setOut(new PrintWriter(out))
  .setErr(new PrintWriter(err))
  .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.OFF))

  String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()

  def "tib generate alone prints its usage"() {
    expect:
    tib.execute('generate') == 0
    output().startsWith('Usage: tib generate')
  }

  def "the spec is required"() {
    expect:
    tib.execute('generate', 'entity') == 2
    error().startsWith("Missing required option: '--spec=FILE'")
  }

  def "a directory that is not a tib project is refused"() {
    expect:
    tib.execute('generate', 'entity', '--spec', spec, '-d', dir.toString()) == 1
    error() == "${dir.toAbsolutePath().normalize()} is not a tib project: there is no .tib/state.yaml. Run tib init service first.\n"
  }

  def "generates the entity layer into the service, once"() {
    given:
    tib.execute('init', 'service', '--spec', spec, '-d', dir.toString())
    out.buffer.length = 0

    when:
    int dryRun = tib.execute('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then:
    dryRun == 0
    output().contains('+++ b/src/main/java/com/example/vehicleservice/vehicle/VehicleGenerated.java\n')
    Files.notExists(dir.resolve('src/main/java/com/example/vehicleservice/vehicle'))

    when:
    out.buffer.length = 0
    int run = tib.execute('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    run == 0
    output() == "Wrote 17 files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/resources/db/migration/V2__create_vehicle.sql'))
    State.read(Files.readString(dir.resolve(State.PATH))).files().size() == 24

    when: 'again'
    out.buffer.length = 0
    int again = tib.execute('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    again == 0
    output() == 'Nothing to change.\n'
  }

  /** What the command printed, with the line separator of every platform. */
  private String output() {
    out.toString().replace('\r\n', '\n')
  }

  private String error() {
    err.toString().replace('\r\n', '\n')
  }

  def "a problem of the spec is reported"() {
    given:
    Path bad = Files.writeString(dir.resolve('bad.yaml'), 'tib: 1\nservice: s\npack: nope\n')

    expect:
    tib.execute('generate', 'entity', '--spec', bad.toString(), '-d', dir.toString()) == 1
    error() == "there is no pack 'nope'\n"
  }
}
