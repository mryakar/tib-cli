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

class GenerateEntityCommandSpec extends Specification {

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
    result.out() == ''
    result.err().contains('Usage: tib generate entity ')
    result.err().contains('--spec=FILE')
    result.err().contains('--ddl=FILE')
  }

  def "the spec and the DDL exclude each other"() {
    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '--ddl', ddl)

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().contains('Usage: tib generate entity ')
  }

  def "a directory that is not a tib project is refused"() {
    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == "${dir.toAbsolutePath().normalize()} is not a tib project: there is no .tib/state.yaml\nRun tib init service first.\n"
  }

  def "a dry run prints the diff and writes nothing"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    Map<String, String> before = Tib.files(dir)

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then:
    result.exitCode() == 0
    result.out().contains('+++ b/src/main/java/com/example/vehicleservice/vehicle/VehicleGenerated.java\n')
    result.out().contains('+++ b/.tib/state.yaml\n')
    result.err() == ''
    Tib.files(dir) == before
  }

  def "generates the entity layer of the spec into the service, once"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    State before = Tib.state(dir)

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${Tib.state(dir).files().size() - before.files().size() + 1} files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/resources/db/migration/V2__create_vehicle.sql'))

    when: 'again'
    Result again = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then: 'the spec is on record once'
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
    Tib.state(dir).inputs() == [State.Input.of(State.Input.Kind.SPEC, dir, Path.of(spec), Files.readString(Path.of(spec)))]

    when: 'again, as a dry run'
    Result dryRun = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then: 'the diff is empty'
    dryRun.exitCode() == 0
    dryRun.out() == ''
    dryRun.err() == 'Nothing to change.\n'
  }

  def "the files of an entity that left the spec are deleted, but for those that were edited"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    String depot = Files.writeString(dir.resolve('depot.yaml'), Files.readString(Path.of(spec))
        .replaceFirst(/(?s)  - name: Vehicle.*(?=  - name: Depot)/, '')).toString()
    String vehicle = 'src/main/java/com/example/vehicleservice/vehicle/'
    String tests = 'src/test/java/com/example/vehicleservice/vehicle/'
    Files.writeString(dir.resolve(vehicle + 'Vehicle.java'), 'mine\n')
    String migration = Tib.state(dir).files()*.path().find { it.endsWith('__create_vehicle.sql') }
    String notice = """\
      tib no longer generates these files from the spec; they stay, as they were edited:
        ${vehicle}Vehicle.java
      Delete them if the service no longer needs them.
      tib no longer reads the spec from these files, and forgets them:
        ${Tib.state(dir).inputs()[0].path()}
      The spec no longer generates these files, but they last, and tib leaves them:
        ${migration}
      Undo what they made with a file of yours, such as a migration that drops the table.
      """.stripIndent()

    when:
    Result dryRun = tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString(), '--dry-run')

    then: 'the diff deletes them, and nothing is deleted'
    dryRun.exitCode() == 0
    dryRun.out().contains("--- a/${vehicle}VehicleGenerated.java\n+++ /dev/null\n")
    dryRun.out().contains("--- a/${vehicle}VehicleService.java\n+++ /dev/null\n")
    dryRun.err() == notice
    Files.exists(dir.resolve(vehicle + 'VehicleGenerated.java'))

    when:
    Result result = tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == """\
      Wrote 1 file to ${dir.toAbsolutePath().normalize()}.
      Deleted 10 files that the spec no longer generates:
        ${vehicle}VehicleController.java
        ${vehicle}VehicleDto.java
        ${vehicle}VehicleGenerated.java
        ${vehicle}VehicleMapper.java
        ${vehicle}VehicleRepository.java
        ${vehicle}VehicleService.java
        ${tests}VehicleApiTests.java
        ${tests}VehicleRepositoryTests.java
        ${tests}VehicleSamples.java
        ${tests}VehicleServiceTests.java
      """.stripIndent()
    result.err() == notice
    Files.readString(dir.resolve(vehicle + 'Vehicle.java')) == 'mine\n'
    !Files.exists(dir.resolve(vehicle + 'VehicleService.java'))
    !Files.exists(dir.resolve(tests))

    and: 'the migration lasts, on record, and the file that stayed has left the record'
    Files.exists(dir.resolve(migration))
    Tib.state(dir).files()*.path().contains(migration)
    !Tib.state(dir).files()*.path().any { it.startsWith(vehicle) }

    when: 'again'
    Result again = tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString())

    then: 'the file that stayed is named once'
    again.out() == 'Nothing to change.\n'
    again.err() == ''
  }

  def "generates the entity layer of the tables of the DDL into the service, once"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    State before = Tib.state(dir)

    when:
    Result result = tib.run('generate', 'entity', '--ddl', ddl, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${Tib.state(dir).files().size() - before.files().size() + 1} files to ${dir.toAbsolutePath().normalize()}.\n"
    Files.exists(dir.resolve('src/main/java/com/example/vehicleservice/depot/DepotGenerated.java'))
    Tib.state(dir).spec().entities()*.name() == ['Depot', 'Vehicle']
    Tib.state(dir).inputs() == [State.Input.of(State.Input.Kind.DDL, dir, Path.of(ddl), Files.readString(Path.of(ddl)))]

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
    Tib.state(dir).tib() == '1.2.4'
    Tib.state(dir).inputs()*.path() == ['spec.yaml']
  }

  def "the files that the spec was read from are recorded, each once, with their last content"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    Path specFile = Files.writeString(dir.resolve('spec.yaml'), 'tib: 1\nservice: vehicle-service\n')
    tib.run('generate', 'entity', '--spec', specFile.toString(), '-d', dir.toString())
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.run('generate', 'entity', '--ddl', schema.toString(), '-d', dir.toString())

    when: 'the DDL changes, and is read again'
    Files.writeString(schema, Files.readString(schema) + '\nCREATE TABLE DRIVER (ID BIGINT PRIMARY KEY);\n')
    Result result = tib.run('generate', 'entity', '--ddl', schema.toString(), '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.err() == ''
    Tib.state(dir).inputs() == [
      State.Input.of(State.Input.Kind.SPEC, dir, specFile, Files.readString(specFile)),
      State.Input.of(State.Input.Kind.DDL, dir, schema, Files.readString(schema))
    ]
    Tib.state(dir).inputs()*.path() == ['spec.yaml', 'schema.sql']
  }

  def "a spec file is the whole spec, so a run with one forgets the files that the spec was read from before"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.run('generate', 'entity', '--ddl', schema.toString(), '-d', dir.toString())
    Path specFile = Files.copy(Path.of(spec), dir.resolve('spec.yaml'))
    String forgotten = """\
      tib no longer reads the spec from these files, and forgets them:
        schema.sql
      """.stripIndent()

    when:
    Result dryRun = tib.run('generate', 'entity', '--spec', specFile.toString(), '-d', dir.toString(), '--dry-run')

    then: 'the spec file names the tables in its own case, which the migrations do not follow'
    dryRun.exitCode() == 0
    dryRun.err().startsWith(forgotten)
    dryRun.err().contains('-CREATE TABLE depots (\n+CREATE TABLE DEPOTS (\n')

    when:
    Result result = tib.run('generate', 'entity', '--spec', specFile.toString(), '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.err() == dryRun.err()
    Tib.state(dir).inputs()*.path() == ['spec.yaml']
    Tib.state(dir).spec().entities()*.name() == ['Vehicle', 'Depot']
  }

  def "a table whose columns changed after its migration was written is named, with the diff, once"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    String colored = Files.writeString(dir.resolve('colored.yaml'), Files.readString(Path.of(spec)).replace(
            '      - { name: capacity, type: Integer }\n',
            '      - { name: color, type: String, length: 20 }\n      - { name: capacity, type: Integer }\n')).toString()
    String migration = Tib.state(dir).files()*.path().find { it.endsWith('__create_vehicle.sql') }
    String before = Files.readString(dir.resolve(migration))

    when:
    Result result = tib.run('generate', 'entity', '--spec', colored, '-d', dir.toString())

    then: 'the migration stays as it was'
    result.exitCode() == 0
    result.err().contains("""\
      The spec now generates these files otherwise, but they last, and tib leaves them:
      --- a/${migration}
      +++ b/${migration}
      """.stripIndent())
    result.err().contains('+  COLOR VARCHAR(20),\n')
    result.err().endsWith('Make the change with a file of yours, such as a new migration.\n')
    Files.readString(dir.resolve(migration)) == before

    when: 'again'
    Result again = tib.run('generate', 'entity', '--spec', colored, '-d', dir.toString())

    then:
    again.out() == 'Nothing to change.\n'
    again.err() == ''
  }

  def "the migration of an entity that comes back to the spec is named, once: #how"() {
    given: 'the vehicle left the spec'
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    String depot = Files.writeString(dir.resolve('depot.yaml'), Files.readString(Path.of(spec))
        .replaceFirst(/(?s)  - name: Vehicle.*(?=  - name: Depot)/, '')).toString()
    tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString())
    String migration = Tib.state(dir).files()*.path().find { it.endsWith('__create_vehicle.sql') }
    if (content != null) {
      Files.writeString(dir.resolve(migration), content)
    }
    String before = Files.readString(dir.resolve(migration))

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then: 'the migration stays as it is'
    result.exitCode() == 0
    result.err().contains('The spec generates these files again, but tib wrote them before, and leaves them:\n' + named(migration))
    result.err().endsWith('Make sure the database has what they make, with a file of yours, such as a new migration.\n')
    Files.readString(dir.resolve(migration)) == before

    when: 'again'
    Result again = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    again.out() == 'Nothing to change.\n'
    again.err() == ''

    where:
    how                         | content  | named
    'as the spec generates it'  | null     | { "  $it\nMake sure" }
    'with the diff when edited' | 'mine\n' | { "--- a/$it\n+++ b/$it\n@@ -1 +1,".toString() }
  }

  def "a DDL file that is no longer where tib read it is forgotten by the next run"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.run('generate', 'entity', '--ddl', schema.toString(), '-d', dir.toString())
    Files.createDirectories(dir.resolve('db'))
    Path moved = Files.move(schema, dir.resolve('db/schema.sql'))

    when:
    Result result = tib.run('generate', 'entity', '--ddl', moved.toString(), '-d', dir.toString())

    then: 'the entities stay'
    result.exitCode() == 0
    result.out() == "Wrote 1 file to ${dir.toAbsolutePath().normalize()}.\n"
    result.err() == """\
      tib no longer reads the spec from these files, and forgets them:
        schema.sql
      """.stripIndent()
    Tib.state(dir).inputs()*.path() == ['db/schema.sql']
    Tib.state(dir).spec().entities()*.name() == ['Depot', 'Vehicle']
  }

  def "a service generated for #stack is refused, and tib status still reads it"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    Path state = dir.resolve('.tib/state.yaml')
    String pack = Tib.state(dir).pack().version()
    Files.writeString(state, Files.readString(state).replace('  stack: Spring Boot 4\n', line))

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == """\
      ${dir.toAbsolutePath().normalize()} was generated for $stack by tib 1.2.3; pack default $pack generates for Spring Boot 4
      Go on with tib 1.2.3 for this service, or move it to Spring Boot 4 by hand.
      """.stripIndent()
    tib.run('status', '-d', dir.toString()).exitCode() == 0

    where:
    stack               | line
    'an earlier stack'  | ''
    'Spring Boot 3'     | '  stack: Spring Boot 3\n'
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
    'a pack that is not there'      | '--spec' | 'spec.yaml'         | 'tib: 1\nservice: vehicle-service\npack: nope\n' | "there is no pack 'nope'"
    'a spec file that is not there' | '--spec' | 'no-such-spec.yaml' | null                               | 'cannot read no-such-spec.yaml: no such file'
    'DDL that tib cannot use'       | '--ddl'  | 'tables.sql'        | 'CREATE TABLE T (A INT);\n'        | 'the DDL has a problem: T: there is no primary key'
    'a DDL file that is not there'  | '--ddl'  | 'no-such-ddl.sql'   | null                               | 'cannot read no-such-ddl.sql: no such file'
  }

  def "the spec of another #what is refused, and nothing is written"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    String file = Files.writeString(dir.resolve('spec.yaml'), content).toString()
    String before = Files.readString(dir.resolve(State.PATH))

    when:
    Result result = tib.run('generate', 'entity', '--spec', file, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err() == "$file $message\n"
    Files.readString(dir.resolve(State.PATH)) == before

    where:
    what      | content                                                    | message
    'service' | 'tib: 1\nservice: fleet-service\n'                         | 'is the spec of fleet-service; this project is vehicle-service'
    'group'   | 'tib: 1\nservice: vehicle-service\ngroup: com.acme\n'      | 'is the spec of a service in com.acme; this project is in com.example'
  }

  def "a state file that tib cannot read is reported: #state"() {
    given:
    Files.createDirectories(dir.resolve('.tib'))
    Files.writeString(dir.resolve(State.PATH), state)

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err().startsWith("${dir.toAbsolutePath().normalize()}: $message")
    result.err().readLines().size() == 1

    where:
    state                                                                         | message
    '['                                                                           | 'the state file has a problem: state: '
    'tib: 1.2.3\npack: { name: default, version: 1 }\nspec:\n  tib: 1\n  service: int\n' | "the state file has a problem: spec.service: 'int' makes the package 'int'"
  }
}
