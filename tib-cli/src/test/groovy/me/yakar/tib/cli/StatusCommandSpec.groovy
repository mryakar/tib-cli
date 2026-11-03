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

class StatusCommandSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  static final String ENTITY = 'src/main/java/com/example/vehicleservice/vehicle/'

  @TempDir
  Path dir

  Tib tib = new Tib()

  def "a directory that is not a tib project is refused"() {
    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == "${dir.toAbsolutePath().normalize()} is not a tib project: there is no .tib/state.yaml. Run tib init service first.\n"
  }

  def "a service whose files are as tib wrote them"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == '''\
      vehicle-service: tib 1.2.3, pack default 0.1.0, 0 entities, 9 files.
      Every file of tib's is as it wrote it.
      '''.stripIndent()
    result.err() == ''
  }

  def "a file of yours that was edited is as it should be"() {
    given:
    generated()
    Files.writeString(dir.resolve(ENTITY + 'Vehicle.java'), 'edited\n')

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out().endsWith("Every file of tib's is as it wrote it.\n")
  }

  def "a file of yours that was removed is reported, and is no problem"() {
    given:
    generated()
    Files.delete(dir.resolve(ENTITY + 'VehicleController.java'))

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out().readLines().drop(1) == [
      "removed  ${ENTITY}VehicleController.java",
      '',
      'A removed file was yours: tib does not write it again.'
    ]
  }

  def "a file of tib's that was edited or is missing is reported with exit code 1, until tib runs again"() {
    given:
    String spec = generated()
    Files.writeString(dir.resolve(ENTITY + 'VehicleGenerated.java'), 'edited\n')
    Files.delete(dir.resolve(ENTITY + 'VehicleDto.java'))
    Files.delete(dir.resolve(ENTITY + 'VehicleController.java'))

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == """\
      vehicle-service: tib 1.2.3, pack default 0.1.0, 2 entities, ${state().files().size()} files.
      removed  ${ENTITY}VehicleController.java
      missing  ${ENTITY}VehicleDto.java
      edited   ${ENTITY}VehicleGenerated.java

      An edited file is written by tib on every run: its next run replaces it.
      A missing file is one that tib needs: its next run writes it again.
      A removed file was yours: tib does not write it again.
      """.stripIndent()
    result.err() == ''

    when: 'tib runs again'
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    Result again = tib.run('status', '-d', dir.toString())

    then:
    again.exitCode() == 0
  }

  def "the count of entities and files is #what"() {
    given:
    Files.createDirectories(dir.resolve('.tib'))
    Files.writeString(dir.resolve(State.PATH), state)

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.out().readLines()[0] == "s: tib 1, pack p 2, $what."

    where:
    what                 | state
    '0 entities, 0 files' | 'tib: 1\npack: {name: p, version: 2}\nspec: {tib: 1, service: s}\n'
    '1 entity, 1 file'   | 'tib: 1\npack: {name: p, version: 2}\nspec: {tib: 1, service: s, entities: [{name: A}]}\nfiles: [{path: A.java, owner: developer, sha256: x}]\n'
  }

  def "a state file that tib cannot read is reported"() {
    given:
    Files.createDirectories(dir.resolve('.tib'))
    Files.writeString(dir.resolve(State.PATH), '[')

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err().startsWith('.tib/state.yaml: ')
  }

  /** Scaffolds the example service and generates its entities; the spec file. */
  private String generated() {
    String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    spec
  }

  /** The state file of the service. */
  private State state() {
    State.parse(Files.readString(dir.resolve(State.PATH)))
  }
}
