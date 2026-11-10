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

import static me.yakar.tib.cli.Tib.EXAMPLES

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.cli.Tib.Result
import me.yakar.tib.state.State
import spock.lang.Specification
import spock.lang.TempDir

class StatusCommandSpec extends Specification {

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
    result.err() == "${dir.toAbsolutePath().normalize()} is not a tib project: there is no .tib/state.yaml\nRun tib init service first.\n"
  }

  def "a state file that a newer tib wrote is refused"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())

    when:
    Result result = new Tib('1.2.2').run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == "${dir.toAbsolutePath().normalize()}: .tib/state.yaml was written by tib 1.2.3, which is newer than this tib 1.2.2; upgrade tib\n"
  }

  def "a service whose files are as tib wrote them"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == """\
      vehicle-service: tib 1.2.3, pack default ${Tib.state(dir).pack().version()}, 0 entities, ${Tib.state(dir).files().size()} files.
      Every file of tib's is as it wrote it.
      """.stripIndent()
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
      'Every file that tib read the spec from is as it read it.',
      '',
      'Files:',
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
      vehicle-service: tib 1.2.3, pack default ${Tib.state(dir).pack().version()}, 2 entities, ${Tib.state(dir).files().size()} files.
      Every file that tib read the spec from is as it read it.

      Files:
      removed  ${ENTITY}VehicleController.java
      missing  ${ENTITY}VehicleDto.java
      edited   ${ENTITY}VehicleGenerated.java

      An edited file is written by tib on every run: git checkout restores it, or the next run of tib replaces it, with --allow-dirty while the edit is not committed.
      A missing file is one that tib needs: git checkout restores it, or the next run of tib writes it again, with --allow-dirty while the deletion is not committed.
      A removed file was yours: tib does not write it again.
      """.stripIndent()
    result.err() == ''

    when: 'tib runs again'
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    Result again = tib.run('status', '-d', dir.toString())

    then:
    again.exitCode() == 0
  }

  def "an entity that left the spec leaves nothing of tib's to report"() {
    given:
    String spec = generated()
    Path depot = Files.writeString(dir.resolve('depot.yaml'), Files.readString(Path.of(spec))
        .replaceFirst(/(?s)  - name: Vehicle.*(?=  - name: Depot)/, ''))
    tib.run('generate', 'entity', '--spec', depot.toString(), '-d', dir.toString())

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    !Files.exists(dir.resolve(ENTITY))
    result.exitCode() == 0
    result.out().contains("Every file of tib's is as it wrote it.\n")
    !result.out().contains('Files:')
  }

  def "a spec file that was changed is reported with exit code 1, until tib runs with it again"() {
    given:
    Path spec = Files.copy(EXAMPLES.resolve('vehicle-service.yaml'), dir.resolve('vehicle-service.yaml'))
    generated(spec.toString())
    Files.writeString(spec, Files.readString(spec) + '# changed\n')

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == """\
      vehicle-service: tib 1.2.3, pack default ${Tib.state(dir).pack().version()}, 2 entities, ${Tib.state(dir).files().size()} files.
      Every file of tib's is as it wrote it.

      Read from:
      changed  vehicle-service.yaml

      A changed input was edited after tib read the spec from it: the files of tib's follow it once tib runs with it again.
      """.stripIndent()
    result.err() == ''

    when: 'tib runs with it again'
    tib.run('generate', 'entity', '--spec', spec.toString(), '-d', dir.toString())
    Result again = tib.run('status', '-d', dir.toString())

    then:
    again.exitCode() == 0
    again.out().readLines().drop(1) == [
      'Every file that tib read the spec from is as it read it.',
      "Every file of tib's is as it wrote it."
    ]
  }

  def "a spec file that is no longer where tib read it is reported, and is no problem"() {
    given:
    Path spec = Files.copy(EXAMPLES.resolve('vehicle-service.yaml'), dir.resolve('vehicle-service.yaml'))
    generated(spec.toString())
    Files.delete(spec)
    Files.writeString(dir.resolve(ENTITY + 'VehicleGenerated.java'), 'edited\n')

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out().readLines().drop(1) == [
      '',
      'Read from:',
      'absent   vehicle-service.yaml',
      '',
      'Files:',
      "edited   ${ENTITY}VehicleGenerated.java",
      '',
      'An absent input is no longer where tib read it: tib does not need it, and keeps its record.',
      'An edited file is written by tib on every run: git checkout restores it, or the next run of tib replaces it, with --allow-dirty while the edit is not committed.'
    ]

    when: 'the file of tib\'s is as it wrote it again'
    tib.run('generate', 'entity', '--spec', EXAMPLES.resolve('vehicle-service.yaml').toString(), '-d', dir.toString())
    Result again = tib.run('status', '-d', dir.toString())

    then:
    again.exitCode() == 0
    again.out().contains('absent   vehicle-service.yaml\n')
  }

  def "a spec file that is there but cannot be read is reported"() {
    given:
    Path spec = Files.copy(EXAMPLES.resolve('vehicle-service.yaml'), dir.resolve('vehicle-service.yaml'))
    generated(spec.toString())
    Files.delete(spec)
    Files.createDirectory(spec)

    when:
    Result result = tib.run('status', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err().startsWith("cannot read ${dir.toAbsolutePath().normalize().resolve('vehicle-service.yaml')}: ")
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
    result.err().startsWith("${dir.toAbsolutePath().normalize()}: the state file has a problem: state: ")
  }

  /** Scaffolds the service of the spec and generates its entities; the spec file. */
  private String generated(String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()) {
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    spec
  }
}
