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

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import me.yakar.tib.cli.Tib.Result
import me.yakar.tib.state.State
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

class GenerateEntityCommandSpec extends Specification {

  @TempDir
  Path dir

  Tib tib

  def setup() {
    tib = new Tib(dir)
  }

  String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()

  String ddl = EXAMPLES.resolve('vehicle-service.sql').toString()

  /** The spec of the documentation with a column more on its vehicle. */
  static final String COLORED = Files.readString(Path.of(Tib.SPEC)).replace(
  '      - { name: capacity, type: Integer }\n',
  '      - { name: color, type: String, length: 20 }\n      - { name: capacity, type: Integer }\n')

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

  def "a dry run prints the diff and writes nothing"() {
    given:
    tib.init('--spec', spec)
    Map<String, String> before = tib.files()

    when:
    Result result = tib.generate('--spec', spec, '--dry-run')

    then:
    result.exitCode() == 0
    result.out().contains('+++ b/src/main/java/com/example/vehicleservice/vehicle/VehicleGenerated.java\n')
    result.out().contains('+++ b/.tib/state.yaml\n')
    result.err() == ''
    tib.files() == before
  }

  def "generates the entity layer of the spec into the service, once"() {
    given:
    tib.init('--spec', spec)
    State before = tib.state()

    when:
    Result result = tib.generate('--spec', spec)

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${tib.state().files().size() - before.files().size() + 1} files to ${tib.root}.\n"
    Files.exists(dir.resolve('src/main/resources/db/migration/V2__create_vehicle.sql'))

    when: 'again'
    Result again = tib.generate('--spec', spec)

    then: 'the spec is on record once'
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
    tib.state().inputs() == [State.Input.of(State.Input.Kind.SPEC, dir, Path.of(spec), Files.readString(Path.of(spec)))]

    when: 'again, as a dry run'
    Result dryRun = tib.generate('--spec', spec, '--dry-run')

    then: 'the diff is empty'
    dryRun.exitCode() == 0
    dryRun.out() == ''
    dryRun.err() == 'Nothing to change.\n'
  }

  def "the files of an entity that left the spec are deleted, but for those that were edited"() {
    given:
    tib.init('--spec', spec)
    tib.generate('--spec', spec)
    String depot = tib.write('depot.yaml', Tib.DEPOT_ONLY)
    String vehicle = 'src/main/java/com/example/vehicleservice/vehicle/'
    String tests = 'src/test/java/com/example/vehicleservice/vehicle/'
    Files.writeString(dir.resolve(vehicle + 'Vehicle.java'), 'mine\n')
    Map<String, String> before = tib.files()
    String migration = tib.state().files()*.path().find { it.endsWith('__create_vehicle.sql') }
    String notice = """\
      tib no longer generates these files from the spec; they stay, as they were edited:
        ${vehicle}Vehicle.java
      Delete them if the service no longer needs them.
      tib no longer reads the spec from these files, and forgets them:
        ${tib.state().inputs()[0].path()}
      The spec no longer generates these files, but they last, and tib leaves them:
        ${migration}
      Undo what they made with a file of yours, such as a migration that drops the table.
      """.stripIndent()

    when:
    Result dryRun = tib.generate('--spec', depot, '--dry-run')

    then: 'the diff deletes them, and nothing is deleted'
    dryRun.exitCode() == 0
    dryRun.out().contains("--- a/${vehicle}VehicleGenerated.java\n+++ /dev/null\n")
    dryRun.out().contains("--- a/${vehicle}VehicleService.java\n+++ /dev/null\n")
    dryRun.err() == notice
    tib.files() == before

    when:
    Result result = tib.generate('--spec', depot)

    then:
    result.exitCode() == 0
    result.out() == """\
      Wrote 1 file to ${tib.root}.
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
    tib.state().files()*.path().contains(migration)
    !tib.state().files()*.path().any { it.startsWith(vehicle) }

    when: 'again'
    Result again = tib.generate('--spec', depot)

    then: 'the file that stayed is named once'
    again.out() == 'Nothing to change.\n'
    again.err() == ''
  }

  def "a dry run of #scenario writes nothing, and a second run changes nothing"() {
    given:
    List<String> source = change(tib)
    Map<String, String> before = tib.files()

    when:
    Result dryRun = tib.generate(*source, '--dry-run')

    then:
    dryRun.exitCode() == 0
    dryRun.out().contains('+++ b/.tib/state.yaml\n')
    dryRun.err() != ''
    tib.files() == before

    when:
    Result result = tib.generate(*source)

    then: 'the run names what the dry run named'
    result.exitCode() == 0
    result.out().startsWith('Wrote ')
    result.err() == dryRun.err()
    tib.files() != before

    when: 'again'
    Result again = tib.generate(*source)

    then:
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
    again.err() == ''

    where:
    scenario                                  | change
    'an entity that left the spec'            | { Tib tib ->
      tib.init('--spec', Tib.SPEC)
      tib.generate('--spec', Tib.SPEC)
      ['--spec', tib.write('depot.yaml', Tib.DEPOT_ONLY)]
    }
    'a spec file that replaces the DDL'       | { Tib tib ->
      tib.init('vehicle-service')
      tib.generate('--ddl', tib.write('schema.sql', Files.readString(Path.of(Tib.DDL))))
      ['--spec', tib.write('spec.yaml', Files.readString(Path.of(Tib.SPEC)))]
    }
    'DDL that was moved'                      | { Tib tib ->
      tib.init('vehicle-service')
      Path schema = Path.of(tib.write('schema.sql', Files.readString(Path.of(Tib.DDL))))
      tib.generate('--ddl', schema.toString())
      Files.createDirectories(tib.service.resolve('db'))
      ['--ddl', Files.move(schema, tib.service.resolve('db/schema.sql')).toString()]
    }
    'a table whose columns changed'           | { Tib tib ->
      tib.init('--spec', Tib.SPEC)
      tib.generate('--spec', Tib.SPEC)
      [
        '--spec',
        tib.write('colored.yaml', Files.readString(Path.of(Tib.SPEC)).replace(
            '      - { name: capacity, type: Integer }\n',
            '      - { name: color, type: String, length: 20 }\n      - { name: capacity, type: Integer }\n'))
      ]
    }
    'an entity that came back to the spec'    | { Tib tib ->
      tib.init('--spec', Tib.SPEC)
      tib.generate('--spec', Tib.SPEC)
      tib.generate('--spec', tib.write('depot.yaml', Tib.DEPOT_ONLY))
      ['--spec', Tib.SPEC]
    }
  }

  def "generates the entity layer of the tables of the DDL into the service, once"() {
    given:
    tib.init('vehicle-service')
    State before = tib.state()

    when:
    Result result = tib.generate('--ddl', ddl)

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${tib.state().files().size() - before.files().size() + 1} files to ${tib.root}.\n"
    Files.exists(dir.resolve('src/main/java/com/example/vehicleservice/depot/DepotGenerated.java'))
    tib.state().spec().entities()*.name() == ['Depot', 'Vehicle']
    tib.state().inputs() == [State.Input.of(State.Input.Kind.DDL, dir, Path.of(ddl), Files.readString(Path.of(ddl)))]

    when: 'again'
    Result again = tib.generate('--ddl', ddl)

    then:
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
  }

  def "a run that changes only the state file writes one file"() {
    given: 'a service that an older tib scaffolded, and a spec without entities'
    tib.init('vehicle-service')
    Path empty = Files.writeString(dir.resolve('spec.yaml'), 'tib: 1\nservice: vehicle-service\n')

    when:
    Result result = new Tib(dir, '1.2.4').generate('--spec', empty.toString())

    then:
    result.exitCode() == 0
    result.out() == "Wrote 1 file to ${tib.root}.\n"
    tib.state().tib() == '1.2.4'
    tib.state().inputs()*.path() == ['spec.yaml']
  }

  def "the files that the spec was read from are recorded, each once, with their last content"() {
    given:
    tib.init('vehicle-service')
    Path specFile = Files.writeString(dir.resolve('spec.yaml'), 'tib: 1\nservice: vehicle-service\n')
    tib.generate('--spec', specFile.toString())
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.generate('--ddl', schema.toString())

    when: 'the DDL changes, and is read again'
    Files.writeString(schema, Files.readString(schema) + '\nCREATE TABLE DRIVER (ID BIGINT PRIMARY KEY);\n')
    Result result = tib.generate('--ddl', schema.toString())

    then:
    result.exitCode() == 0
    result.err() == ''
    tib.state().inputs() == [
      State.Input.of(State.Input.Kind.SPEC, dir, specFile, Files.readString(specFile)),
      State.Input.of(State.Input.Kind.DDL, dir, schema, Files.readString(schema))
    ]
    tib.state().inputs()*.path() == ['spec.yaml', 'schema.sql']
  }

  def "a spec file is the whole spec, so a run with one forgets the files that the spec was read from before"() {
    given:
    tib.init('vehicle-service')
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.generate('--ddl', schema.toString())
    Path specFile = Files.copy(Path.of(spec), dir.resolve('spec.yaml'))
    String forgotten = """\
      tib no longer reads the spec from these files, and forgets them:
        schema.sql
      """.stripIndent()

    when:
    Result dryRun = tib.generate('--spec', specFile.toString(), '--dry-run')

    then: 'the spec file names the tables in its own case, which the migrations do not follow'
    dryRun.exitCode() == 0
    dryRun.err().startsWith(forgotten)
    dryRun.err().contains('-CREATE TABLE depots (\n+CREATE TABLE DEPOTS (\n')

    when:
    Result result = tib.generate('--spec', specFile.toString())

    then:
    result.exitCode() == 0
    result.err() == dryRun.err()
    tib.state().inputs()*.path() == ['spec.yaml']
    tib.state().spec().entities()*.name() == ['Vehicle', 'Depot']
  }

  def "a table whose columns changed after its migration was written is named, with the diff, once"() {
    given:
    tib.init('--spec', spec)
    tib.generate('--spec', spec)
    String colored = Files.writeString(dir.resolve('colored.yaml'), Files.readString(Path.of(spec)).replace(
            '      - { name: capacity, type: Integer }\n',
            '      - { name: color, type: String, length: 20 }\n      - { name: capacity, type: Integer }\n')).toString()
    String migration = tib.state().files()*.path().find { it.endsWith('__create_vehicle.sql') }
    String before = Files.readString(dir.resolve(migration))

    when:
    Result result = tib.generate('--spec', colored)

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
    Result again = tib.generate('--spec', colored)

    then:
    again.out() == 'Nothing to change.\n'
    again.err() == ''
  }

  def "the migrations of a service that was scaffolded from a spec are written without a notice: #scenario"() {
    given: 'the spec is on record, and none of its migrations is written'
    tib.init('--spec', spec)
    String other = tib.write('other.yaml', yaml)
    String forgotten = "tib no longer reads the spec from these files, and forgets them:\n  ${tib.state().inputs()[0].path()}\n"

    when:
    Result result = tib.generate('--spec', other, *options)

    then:
    result.exitCode() == 0
    result.err() == forgotten

    where:
    scenario                        | yaml           | options
    'a column more'                 | COLORED        | []
    'a column more, in a dry run'   | COLORED        | ['--dry-run']
    'an entity fewer'               | Tib.DEPOT_ONLY | []
    'an entity fewer, in a dry run' | Tib.DEPOT_ONLY | ['--dry-run']
  }

  def "the migration of an entity that comes back to the spec is named, once: #how"() {
    given: 'the vehicle left the spec'
    tib.init('--spec', spec)
    tib.generate('--spec', spec)
    String depot = tib.write('depot.yaml', Tib.DEPOT_ONLY)
    tib.generate('--spec', depot)
    String migration = tib.state().files()*.path().find { it.endsWith('__create_vehicle.sql') }
    if (content != null) {
      Files.writeString(dir.resolve(migration), content)
    }
    String before = Files.readString(dir.resolve(migration))

    when:
    Result result = tib.generate('--spec', spec)

    then: 'the migration stays as it is'
    result.exitCode() == 0
    result.err().contains('The spec generates these files, but they are there or tib wrote them before, and tib leaves them:\n' + named(migration))
    result.err().endsWith('Make sure the database has what they make, with a file of yours, such as a new migration.\n')
    Files.readString(dir.resolve(migration)) == before

    when: 'again'
    Result again = tib.generate('--spec', spec)

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
    tib.init('vehicle-service')
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.generate('--ddl', schema.toString())
    Files.createDirectories(dir.resolve('db'))
    Path moved = Files.move(schema, dir.resolve('db/schema.sql'))

    when:
    Result result = tib.generate('--ddl', moved.toString())

    then: 'the entities stay'
    result.exitCode() == 0
    result.out() == "Wrote 1 file to ${tib.root}.\n"
    result.err() == """\
      tib no longer reads the spec from these files, and forgets them:
        schema.sql
      """.stripIndent()
    tib.state().inputs()*.path() == ['db/schema.sql']
    tib.state().spec().entities()*.name() == ['Depot', 'Vehicle']
  }

  @Requires({
    FileSystems.default.supportedFileAttributeViews().contains('posix') && System.getProperty('user.name') != 'root'
  })
  def "a file of yours that tib cannot read is reported with exit code 1, and nothing is written: #run"() {
    given:
    tib.init('--spec', spec)
    tib.generate('--spec', spec)
    Path vehicle = dir.resolve('src/main/java/com/example/vehicleservice/vehicle/Vehicle.java')
    Files.setPosixFilePermissions(vehicle, PosixFilePermissions.fromString('-w-------'))
    Map<String, Set> before = permissions()

    when:
    Result result = tib.generate('--spec', tib.write('colored.yaml', COLORED), *options)

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == "cannot read $vehicle: access denied\n"
    permissions() == before

    where:
    run         | options
    'a run'     | []
    'a dry run' | ['--dry-run']
  }

  def "a service generated for #stack is refused by #run, and tib status still reads it"() {
    given:
    tib.init('--spec', spec)
    Path state = dir.resolve('.tib/state.yaml')
    String pack = tib.state().pack().version()
    Files.writeString(state, Files.readString(state).replace('  stack: Spring Boot 4\n', line))
    Map<String, String> before = tib.files()

    when:
    Result result = tib.generate('--spec', spec, *(run == 'a dry run' ? ['--dry-run'] : []))

    then: 'nothing is written'
    result.exitCode() == 1
    result.out() == ''
    result.err() == """\
      ${tib.root} was generated for $stack by tib 1.2.3; pack default $pack generates for Spring Boot 4
      Go on with tib 1.2.3 for this service, or move it to Spring Boot 4 by hand.
      """.stripIndent()
    tib.files() == before
    tib.status().exitCode() == 0

    where:
    stack              | line                        | run
    'an earlier stack' | ''                          | 'a run'
    'Spring Boot 3'    | '  stack: Spring Boot 3\n' | 'a run'
    'Spring Boot 3'    | '  stack: Spring Boot 3\n' | 'a dry run'
  }

  def "#problem is reported with exit code 1"() {
    given:
    tib.init('vehicle-service')
    String file = content == null ? name : Files.writeString(dir.resolve(name), content).toString()

    when:
    Result result = tib.generate(option, file)

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
    tib.init('vehicle-service')
    String file = Files.writeString(dir.resolve('spec.yaml'), content).toString()
    String before = Files.readString(dir.resolve(State.PATH))

    when:
    Result result = tib.generate('--spec', file)

    then:
    result.exitCode() == 1
    result.err() == "$file $message\n"
    Files.readString(dir.resolve(State.PATH)) == before

    where:
    what      | content                                                    | message
    'service' | 'tib: 1\nservice: fleet-service\n'                         | 'is the spec of fleet-service; this project is vehicle-service'
    'group'   | 'tib: 1\nservice: vehicle-service\ngroup: com.acme\n'      | 'is the spec of a service in com.acme; this project is in com.example'
  }

  /** The files of the service by path, each with its permissions and when it was last written. */
  private Map<String, Set> permissions() {
    Files.walk(dir).withCloseable { paths ->
      paths.filter {
        Files.isRegularFile(it) && it.fileName.toString() != 'colored.yaml'
      }.toList().collectEntries {
        [
          dir.relativize(it).toString(),
          [Files.getPosixFilePermissions(it), Files.getLastModifiedTime(it)] as Set
        ]
      }
    }
  }
}
