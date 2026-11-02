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
import me.yakar.tib.cli.Tib.Result
import me.yakar.tib.state.State
import spock.lang.Specification
import spock.lang.TempDir

class GenerateEntityCommandSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  @TempDir
  Path dir

  Tib tib = new Tib()

  String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()

  String ddl = EXAMPLES.resolve('vehicle-service.sql').toString()

  def "the spec or the DDL is required"() {
    when:
    Result result = tib.run('generate', 'entity')

    then:
    result.exitCode() == 2
    result.err().startsWith('Error: Missing required argument (specify one of these): (--spec=FILE | --ddl=FILE)')
  }

  def "the spec and the DDL exclude each other"() {
    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '--ddl', ddl)

    then:
    result.exitCode() == 2
    result.err().startsWith('Error: --spec=FILE, --ddl=FILE are mutually exclusive (specify only one)')
  }

  def "a directory that is not a tib project is refused"() {
    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == "${dir.toAbsolutePath().normalize()} is not a tib project: there is no .tib/state.yaml. Run tib init service first.\n"
  }

  def "a dry run prints the diff and writes nothing"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    Map<String, String> before = files()

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then:
    result.exitCode() == 0
    result.out().contains('+++ b/src/main/java/com/example/vehicleservice/vehicle/VehicleGenerated.java\n')
    result.out().contains('+++ b/.tib/state.yaml\n')
    result.err() == ''
    files() == before
  }

  def "generates the entity layer of the spec into the service, once"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    State before = state()

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${state().files().size() - before.files().size() + 1} files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/resources/db/migration/V2__create_vehicle.sql'))

    when: 'again'
    Result again = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'

    when: 'again, as a dry run'
    Result dryRun = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then: 'the diff is empty'
    dryRun.exitCode() == 0
    dryRun.out() == ''
    dryRun.err() == 'Nothing to change.\n'
  }

  def "generates the entity layer of the tables of the DDL into the service, once"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    State before = state()

    when:
    Result result = tib.run('generate', 'entity', '--ddl', ddl, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${state().files().size() - before.files().size() + 1} files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/java/com/example/vehicleservice/depot/DepotGenerated.java'))
    state().spec().entities()*.name() == ['Depot', 'Vehicle']

    when: 'again'
    Result again = tib.run('generate', 'entity', '--ddl', ddl, '-d', dir.toString())

    then:
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
  }

  def "a run that changes only the state file writes one file"() {
    given: 'a service that an older tib scaffolded, and a spec without entities'
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    Path empty = Files.writeString(dir.resolve('spec.yaml'), 'tib: 1\nservice: vehicle-service\n')

    when:
    Result result = new Tib('1.2.4').run('generate', 'entity', '--spec', empty.toString(), '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == "Wrote 1 file to ${dir.toAbsolutePath().normalize()}.\n"
    state().tib() == '1.2.4'
  }

  def "#problem is reported with exit code 1"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    String file = content == null ? name : Files.writeString(dir.resolve(name), content).toString()

    when:
    Result result = tib.run('generate', 'entity', option, file, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == "$message\n"

    where:
    problem                         | option   | name                | content                            | message
    'a spec that tib cannot use'    | '--spec' | 'spec.yaml'         | 'tib: 2\nservice: s\n'             | 'the spec has a problem: tib: 2 is above 1'
    'a pack that is not there'      | '--spec' | 'spec.yaml'         | 'tib: 1\nservice: s\npack: nope\n' | "there is no pack 'nope'"
    'a spec file that is not there' | '--spec' | 'no-such-spec.yaml' | null                               | 'cannot read no-such-spec.yaml: no such file'
    'DDL that tib cannot use'       | '--ddl'  | 'tables.sql'        | 'CREATE TABLE T (A INT);\n'        | 'the DDL has a problem: T: there is no primary key'
    'a DDL file that is not there'  | '--ddl'  | 'no-such-ddl.sql'   | null                               | 'cannot read no-such-ddl.sql: no such file'
  }

  def "a state file that tib cannot read is reported: #option, #state"() {
    given:
    Files.createDirectories(dir.resolve('.tib'))
    Files.writeString(dir.resolve(State.PATH), state)

    when:
    Result result = tib.run('generate', 'entity', option, file, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err().startsWith(message)
    result.err().readLines().size() == 1

    where:
    option   | file                                                | state                                  | message
    '--spec' | EXAMPLES.resolve('vehicle-service.yaml').toString() | '['                                    | '.tib/state.yaml: '
    '--ddl'  | EXAMPLES.resolve('vehicle-service.sql').toString()  | '['                                    | '.tib/state.yaml: '
    '--spec' | EXAMPLES.resolve('vehicle-service.yaml').toString() | 'spec:\n  tib: 1\n  service: int\n' | ".tib/state.yaml: the spec has a problem: service: 'int' makes the package 'int'"
  }

  /** The state file of the service. */
  private State state() {
    State.parse(Files.readString(dir.resolve(State.PATH)))
  }

  /** Every file of the service, with its content, by path. */
  private Map<String, String> files() {
    Files.walk(dir).withCloseable { paths ->
      paths.filter {
        Files.isRegularFile(it)
      }.toList().collectEntries {
        [dir.relativize(it).toString(), Files.readString(it)]
      }
    }
  }
}
