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

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.cli.Tib.Result
import me.yakar.tib.spec.Architecture
import me.yakar.tib.spec.Names
import me.yakar.tib.spec.Spec
import me.yakar.tib.state.State
import me.yakar.tib.tree.Owner
import spock.lang.Specification
import spock.lang.TempDir

class InitServiceCommandSpec extends Specification {

  @TempDir
  Path dir

  Tib tib

  static final String NAME = 'The name of the service, in lower case words joined with hyphens: '
  static final String JAVA = 'Its Java version, 21 or 25 [21]: '
  static final String ARCHITECTURE = 'Its architecture: anemic, ddd [anemic]: '
  static final String TESTS = 'The kinds of its tests, separated by commas, or none: unit, integration, api, archunit [unit,integration,api]: '
  static final String DOCKER = 'A Dockerfile, yes or no [no]: '
  static final String QUESTIONS = NAME + JAVA + ARCHITECTURE + TESTS + DOCKER

  def setup() {
    tib = new Tib(dir)
  }

  def "without the name and the spec, tib asks for them on standard error and reads the answers: #what"() {
    given:
    tib.answers = new ByteArrayInputStream(answers.getBytes('UTF-8'))

    when:
    Result result = tib.init()

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${tib.state().files().size() + 1} files to ${tib.root}.\n"
    result.err() == err
    Spec spec = tib.state().spec()
    spec.service() == 'vehicle-service'
    spec.java() == java
    spec.tests()*.toString() == tests
    spec.docker() == docker
    tib.state().inputs() == []

    where:
    what                                    | answers                                             | java | tests                          | docker | err
    'the defaults'                          | 'vehicle-service\n\n\n\n\n'                         | 21   | ['unit', 'integration', 'api'] | false  | QUESTIONS
    'answers of their own, with spaces'     | ' vehicle-service \n25\n anemic \napi, unit, archunit\nY\n' | 25 | ['api', 'unit', 'archunit'] | true | QUESTIONS
    'none for the tests'                    | 'vehicle-service\n21\n\nnone\nno\n'                 | 21   | []                             | false  | QUESTIONS
    'answers that do not read, asked again' | '\nVehicle Service\nvehicle-service\n17\nx\n25\nlayered\n\nunit,e2e\nunit,unit\nunit\nmaybe\nyes\n' | 25 | ['unit'] | true | NAME + "service: '' does not match ${Names.SERVICE}\n" + NAME + "service: 'Vehicle Service' does not match ${Names.SERVICE}\n" + NAME + JAVA + 'java: 17 is not one of 21, 25\n' + JAVA + "'x' is not a number\n" + JAVA + ARCHITECTURE + "'layered' is not one of anemic, ddd\n" + ARCHITECTURE + TESTS + "'e2e' is not one of unit, integration, api, archunit\n" + TESTS + "tests[1]: 'unit' is already there\n" + TESTS + DOCKER + 'Answer yes or no.\n' + DOCKER
  }

  def "a dry run that asks prints the diff on standard output, and the questions on standard error"() {
    given:
    tib.answers = new ByteArrayInputStream('vehicle-service\n\n\n\n\n'.getBytes('UTF-8'))

    when:
    Result result = tib.init('--dry-run')

    then:
    result.exitCode() == 0
    result.out().startsWith('--- /dev/null\n+++ b/.gitattributes\n')
    result.err().startsWith(QUESTIONS)
    dir.toFile().list().length == 0
  }

  def "without the name, the spec and any answer, the command line is a mistake"() {
    when:
    Result result = tib.run('init', 'service')

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().startsWith(NAME + '\nThe command line leaves out what tib asks for, and there are no more answers.\nUsage: tib init service ')
    result.err().contains('NAME')
    result.err().contains('--spec=FILE')
  }

  def "answers that end before the questions do are a mistake of the command line, and nothing is written"() {
    given:
    tib.answers = new ByteArrayInputStream('vehicle-service\n25\n'.getBytes('UTF-8'))

    when:
    Result result = tib.init()

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().startsWith(NAME + JAVA + ARCHITECTURE + '\nThe command line leaves out what tib asks for, and there are no more answers.\nUsage: tib init service ')
    dir.toFile().list().length == 0
  }

  def "answers that cannot be read are reported"() {
    given:
    tib.answers = new InputStream() {
          @Override
          int read() {
            throw new IOException('the terminal is gone')
          }
        }

    when:
    Result result = tib.init()

    then:
    result.exitCode() == 1
    result.err() == NAME + 'cannot read the answers: the terminal is gone\n'
    dir.toFile().list().length == 0
  }

  def "a directory that is a tib project already is refused before tib asks anything"() {
    given:
    tib.init('vehicle-service')
    tib.answers = new ByteArrayInputStream('other-service\n\n\n\n'.getBytes('UTF-8'))

    when:
    Result result = tib.init()

    then:
    result.exitCode() == 1
    result.err() == "${tib.root} is already a tib project: .tib/state.yaml is there\n"
  }

  def "the name and the spec exclude each other"() {
    when:
    Result result = tib.init('vehicle-service', '--spec', Tib.SPEC)

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().contains('Usage: tib init service ')
    dir.toFile().list().length == 0
  }

  def "--java goes with the name, not with the spec file, which says the Java itself: #args"() {
    when:
    Result result = tib.init(args as String[])

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().contains('Usage: tib init service ')
    dir.toFile().list().length == 0

    where:
    args << [['--spec', Tib.SPEC, '--java', '25'], ['--java', '25']]
  }

  def "the service is on Java #java, as #how says"() {
    when:
    Result result = tib.init(['vehicle-service'] + args as String[])

    then:
    result.exitCode() == 0
    tib.state().spec().java() == java
    Files.readString(dir.resolve('build.gradle.kts')).contains("JavaLanguageVersion.of($java)")

    where:
    how         | args               | java
    'no option' | []                 | 21
    '--java'    | ['--java', '25']   | 25
  }

  def "the service has the kinds of tests that #how says"() {
    when:
    Result result = tib.init(['vehicle-service'] + args as String[])

    then:
    result.exitCode() == 0
    tib.state().spec().tests()*.toString() == tests

    where:
    how         | args                            | tests
    'no option' | []                              | ['unit', 'integration', 'api']
    '--tests'   | ['--tests', 'api,unit']         | ['api', 'unit']
    '--tests'   | ['--tests', 'unit', '--tests', 'api'] | ['unit', 'api']
  }

  def "the service has a Dockerfile with --docker, and none without"() {
    when:
    Result result = tib.init(['vehicle-service'] + args as String[])

    then:
    result.exitCode() == 0
    tib.state().spec().docker() == docker
    Files.exists(dir.resolve('Dockerfile')) == docker
    Files.exists(dir.resolve('.dockerignore')) == docker

    where:
    args         | docker
    []           | false
    ['--docker'] | true
  }

  def "the architecture of the service is the one of --architecture; another is a mistake of the command line"() {
    when:
    Result result = tib.init('vehicle-service', '--architecture', 'ddd')

    then:
    result.exitCode() == 0
    tib.state().spec().architecture() == Architecture.DDD

    when:
    Result other = new Tib(dir.resolve('other')).init('other-service', '--architecture', 'layered')

    then:
    other.exitCode() == 2
    other.err().startsWith("Invalid value for option '--architecture': 'layered' is not one of anemic, ddd\n")
  }

  def "a kind of test that tib does not know is a mistake of the command line"() {
    when:
    Result result = tib.init('vehicle-service', '--tests', 'unit,e2e')

    then:
    result.exitCode() == 2
    result.err().startsWith("Invalid value for option '--tests' (KIND): 'e2e' is not one of unit, integration, api, archunit\n")
    dir.toFile().list().length == 0
  }

  def "a kind of test given twice is reported"() {
    when:
    Result result = tib.init('vehicle-service', '--tests', 'unit,unit')

    then:
    result.exitCode() == 1
    result.err() == "the spec has a problem: tests[1]: 'unit' is already there\n"
  }

  def "a Java that tib does not generate for is reported"() {
    when:
    Result result = tib.init('vehicle-service', '--java', '17')

    then:
    result.exitCode() == 1
    result.err() == 'the spec has a problem: java: 17 is not one of 21, 25\n'
    dir.toFile().list().length == 0
  }

  def "a dry run prints the diff and writes nothing"() {
    when:
    Result result = tib.init('vehicle-service', '--dry-run')

    then:
    result.exitCode() == 0
    result.out().startsWith('--- /dev/null\n+++ b/.gitattributes\n')
    result.out().contains('+++ b/src/main/java/com/example/vehicleservice/VehicleServiceApplication.java\n')
    result.out().contains('Binary files /dev/null and b/gradle/wrapper/gradle-wrapper.jar differ\n')
    dir.toFile().list().length == 0
  }

  def "scaffolds the service and records what it wrote"() {
    when:
    Result result = tib.init('vehicle-service')

    then:
    result.exitCode() == 0
    State state = tib.state()
    result.out() == "Wrote ${state.files().size() + 1} files to ${tib.root}.\n"
    Files.exists(dir.resolve('src/main/java/com/example/vehicleservice/VehicleServiceApplication.java'))
    state.tib() == '1.2.3'
    state.pack().name() == 'default'
    state.inputs() == []
    state.spec().service() == 'vehicle-service'

    and: 'the Gradle wrapper is the developer\'s, and gradlew is a file to run'
    state.files().findAll {
      it.path().startsWith('gradle')
    }.collectEntries {
      [it.path(), it.owner()]
    } == [
      'gradle/wrapper/gradle-wrapper.jar': Owner.DEVELOPER,
      'gradle/wrapper/gradle-wrapper.properties': Owner.DEVELOPER,
      'gradlew': Owner.DEVELOPER,
      'gradlew.bat': Owner.DEVELOPER
    ]
    !FileSystems.default.supportedFileAttributeViews().contains('posix') || Files.isExecutable(dir.resolve('gradlew'))
  }

  def "a directory that is a tib project already is refused"() {
    given:
    tib.init('vehicle-service')

    when:
    Result result = tib.init('vehicle-service')

    then:
    result.exitCode() == 1
    result.err() == "${tib.root} is already a tib project: .tib/state.yaml is there\n"

    when: 'the spec has a problem too'
    Result broken = tib.init('--spec', tib.write('spec.yaml', 'tib: 2\n'))

    then: 'the project is what is reported'
    broken.exitCode() == 1
    broken.err() == result.err()
  }

  def "scaffolds the service of a spec file"() {
    when:
    Result result = tib.init('--spec', Tib.SPEC)

    then:
    result.exitCode() == 0
    State state = tib.state()
    state.spec().entities()*.name() == ['Vehicle', 'Depot']
    state.inputs() == [State.Input.of(State.Input.Kind.SPEC, dir, Path.of(Tib.SPEC), Tib.SPEC_YAML)]
  }

  def "the spec file is recorded with its path relative to the service"() {
    given:
    Path service = Files.createDirectories(dir.resolve('vehicle-service'))
    Files.copy(Path.of(Tib.SPEC), dir.resolve('vehicle-service.yaml'))

    when:
    Result result = new Tib(service).init('--spec', dir.resolve('vehicle-service.yaml').toString())

    then:
    result.exitCode() == 0
    State.Input input = new Tib(service).state().inputs()[0]
    input.kind() == State.Input.Kind.SPEC
    input.path() == '../vehicle-service.yaml'
  }

  def "a name that is not the name of a service is reported"() {
    when:
    Result result = tib.init('Vehicle Service')

    then:
    result.exitCode() == 1
    result.err() == "the spec has a problem: service: 'Vehicle Service' does not match ^[a-z][a-z0-9]*(-[a-z0-9]+)*\$\n"
  }

  def "a spec that tib cannot use is reported: #message"() {
    given:
    String spec = tib.write('spec.yaml', yaml)

    when:
    Result result = new Tib(dir.resolve('service')).init('--spec', spec)

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
    Result result = tib.init('--spec', 'no-such-spec.yaml')

    then:
    result.exitCode() == 1
    result.err() == 'cannot read no-such-spec.yaml: no such file\n'
  }

  def "a directory that cannot be written to is reported"() {
    given:
    Path file = Files.writeString(dir.resolve('file'), '')

    when:
    Result result = new Tib(file).init('s')

    then:
    result.exitCode() == 1
    result.err() == "cannot write ${file.toAbsolutePath().normalize()}: already exists\n"
  }
}
