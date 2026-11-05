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

class InitServiceCommandSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  @TempDir
  Path dir

  Tib tib = new Tib()

  def "the name or the spec is required"() {
    when:
    Result result = tib.run('init', 'service')

    then:
    result.exitCode() == 2
    result.err().startsWith('Error: Missing required argument (specify one of these): (NAME | --spec=FILE)\n')
  }

  def "the name and the spec exclude each other"() {
    when:
    Result result = tib.run('init', 'service', 'vehicle-service', '--spec', EXAMPLES.resolve('vehicle-service.yaml').toString(), '-d', dir.toString())

    then:
    result.exitCode() == 2
    result.err().startsWith('Error: NAME, --spec=FILE are mutually exclusive (specify only one)\n')
    dir.toFile().list().length == 0
  }

  def "a dry run prints the diff and writes nothing"() {
    when:
    Result result = tib.run('init', 'service', 'vehicle-service', '--dir', dir.toString(), '--dry-run')

    then:
    result.exitCode() == 0
    result.out().startsWith('--- /dev/null\n+++ b/.gitattributes\n')
    result.out().contains('+++ b/src/main/java/com/example/vehicleservice/VehicleServiceApplication.java\n')
    dir.toFile().list().length == 0
  }

  def "scaffolds the service and records what it wrote"() {
    when:
    Result result = tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())

    then:
    result.exitCode() == 0
    State state = State.parse(Files.readString(dir.resolve(State.PATH)), '1.2.4')
    result.out() == "Wrote ${state.files().size() + 1} files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/java/com/example/vehicleservice/VehicleServiceApplication.java'))
    state.tib() == '1.2.3'
    state.pack().name() == 'default'
    state.inputs() == []
    state.spec().service() == 'vehicle-service'
  }

  def "a directory that is a tib project already is refused"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())

    when:
    Result result = tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == "${dir.toAbsolutePath().normalize()} is already a tib project: .tib/state.yaml is there.\n"
  }

  def "scaffolds the service of a spec file"() {
    when:
    Result result = tib.run('init', 'service', '--spec', EXAMPLES.resolve('vehicle-service.yaml').toString(), '-d', dir.toString())

    then:
    result.exitCode() == 0
    State state = State.parse(Files.readString(dir.resolve(State.PATH)), '1.2.4')
    state.spec().entities()*.name() == ['Vehicle', 'Depot']
    state.inputs() == [
      State.Input.of(State.Input.Kind.SPEC, dir, EXAMPLES.resolve('vehicle-service.yaml'), Files.readString(EXAMPLES.resolve('vehicle-service.yaml')))
    ]
  }

  def "the spec file is recorded with its path relative to the service"() {
    given:
    Path service = Files.createDirectories(dir.resolve('vehicle-service'))
    Files.copy(EXAMPLES.resolve('vehicle-service.yaml'), dir.resolve('vehicle-service.yaml'))

    when:
    Result result = tib.run('init', 'service', '--spec', dir.resolve('vehicle-service.yaml').toString(), '-d', service.toString())

    then:
    result.exitCode() == 0
    State.Input input = State.parse(Files.readString(service.resolve(State.PATH)), '1.2.4').inputs()[0]
    input.kind() == State.Input.Kind.SPEC
    input.path() == '../vehicle-service.yaml'
  }

  def "a name that is not the name of a service is reported"() {
    when:
    Result result = tib.run('init', 'service', 'Vehicle Service', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == "the spec has a problem: service: 'Vehicle Service' does not match ^[a-z][a-z0-9]*(-[a-z0-9]+)*\$\n"
  }

  def "a spec that tib cannot use is reported: #message"() {
    given:
    Path spec = Files.writeString(dir.resolve('spec.yaml'), yaml)

    when:
    Result result = tib.run('init', 'service', '--spec', spec.toString(), '-d', dir.resolve('service').toString())

    then:
    result.exitCode() == 1
    result.err() == "$message\n"

    where:
    yaml                               | message
    'tib: 1\nservice: s\npack: nope\n' | "there is no pack 'nope'"
    'tib: 2\nservice: s\n'             | 'the spec has a problem: tib: 2 is above 1'
  }

  def "a spec file that is not there is reported"() {
    when:
    Result result = tib.run('init', 'service', '--spec', 'no-such-spec.yaml', '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == 'cannot read no-such-spec.yaml: no such file\n'
  }

  def "a directory that cannot be written to is reported"() {
    given:
    Path file = Files.writeString(dir.resolve('file'), '')

    when:
    Result result = tib.run('init', 'service', 's', '-d', file.toString())

    then:
    result.exitCode() == 1
    result.err() == "cannot write ${file.toAbsolutePath().normalize()}: already exists\n"
  }
}
