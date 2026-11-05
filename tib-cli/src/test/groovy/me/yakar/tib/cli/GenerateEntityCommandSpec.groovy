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
    result.err().contains('is not a tib project')
    result.err().endsWith('\nRun tib init service first.\n')
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

    then: 'the spec is on record once'
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
    state().inputs() == [State.Input.of(State.Input.Kind.SPEC, dir, Path.of(spec), Files.readString(Path.of(spec)))]

    when: 'again, as a dry run'
    Result dryRun = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then: 'the diff is empty'
    dryRun.exitCode() == 0
    dryRun.out() == ''
    dryRun.err() == 'Nothing to change.\n'
  }

  def "the files of an entity that left the spec are forgotten, and named to be deleted"() {
    given:
    tib.run('init', 'service', '--spec', spec, '-d', dir.toString())
    tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())
    String depot = Files.writeString(dir.resolve('depot.yaml'), Files.readString(Path.of(spec))
        .replaceFirst(/(?s)  - name: Vehicle.*(?=  - name: Depot)/, '')).toString()
    String vehicle = 'src/main/java/com/example/vehicleservice/vehicle/'
    String notice = """\
      tib no longer generates these files from the spec, and forgets them:
        ${vehicle}VehicleDto.java
        ${vehicle}VehicleGenerated.java
        ${vehicle}VehicleMapper.java
      Delete them: tib status no longer checks them.
      """.stripIndent()

    when:
    Result dryRun = tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString(), '--dry-run')

    then:
    dryRun.exitCode() == 0
    dryRun.out().contains("-- path: ${vehicle}VehicleDto.java\n")
    dryRun.err() == notice

    when:
    Result result = tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')
    result.err() == notice
    !state().files()*.path().any { it.startsWith(vehicle + 'VehicleGenerated') }

    when: 'again'
    Result again = tib.run('generate', 'entity', '--spec', depot, '-d', dir.toString())

    then: 'they are forgotten once'
    again.out() == 'Nothing to change.\n'
    again.err() == ''
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
    state().inputs() == [State.Input.of(State.Input.Kind.DDL, dir, Path.of(ddl), Files.readString(Path.of(ddl)))]

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
    state().inputs()*.path() == ['spec.yaml']
  }

  def "the files that the spec was read from are recorded, each once, with their last content"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    Path schema = Files.writeString(dir.resolve('schema.sql'), Files.readString(Path.of(ddl)))
    tib.run('generate', 'entity', '--ddl', schema.toString(), '-d', dir.toString())
    Path specFile = Files.writeString(dir.resolve('spec.yaml'), 'tib: 1\nservice: vehicle-service\n')
    tib.run('generate', 'entity', '--spec', specFile.toString(), '-d', dir.toString())

    when: 'the DDL changes, and is read again'
    Files.writeString(schema, Files.readString(schema) + '\nCREATE TABLE DRIVER (ID BIGINT PRIMARY KEY);\n')
    Result result = tib.run('generate', 'entity', '--ddl', schema.toString(), '-d', dir.toString())

    then:
    result.exitCode() == 0
    state().inputs() == [
      State.Input.of(State.Input.Kind.DDL, dir, schema, Files.readString(schema)),
      State.Input.of(State.Input.Kind.SPEC, dir, specFile, Files.readString(specFile))
    ]
    state().inputs()*.path() == ['schema.sql', 'spec.yaml']
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

  def "a state file that tib cannot read is reported: #option, #state"() {
    given:
    Files.createDirectories(dir.resolve('.tib'))
    Files.writeString(dir.resolve(State.PATH), state)

    when:
    Result result = tib.run('generate', 'entity', option, file, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.err().startsWith("${dir.toAbsolutePath().normalize()}: $message")
    result.err().readLines().size() == 1

    where:
    option   | file                                                | state                                                                         | message
    '--spec' | EXAMPLES.resolve('vehicle-service.yaml').toString() | '['                                                                           | 'the state file has a problem: state: '
    '--ddl'  | EXAMPLES.resolve('vehicle-service.sql').toString()  | '['                                                                           | 'the state file has a problem: state: '
    '--spec' | EXAMPLES.resolve('vehicle-service.yaml').toString() | 'tib: 1.2.3\npack: { name: default, version: 1 }\nspec:\n  tib: 1\n  service: int\n' | "the state file has a problem: spec.service: 'int' makes the package 'int'"
  }

  def "a service whose tracked files have uncommitted changes is refused, and nothing is written"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    committed()
    Files.writeString(dir.resolve('AGENTS.md'), 'mine\n')
    Map<String, String> before = files()

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == """\
      ${dir.toAbsolutePath().normalize()} has changes that are not committed:
         M AGENTS.md
      Commit or stash them first, so that the changes of tib can be told apart, or run again with --allow-dirty.
      """.stripIndent()
    files() == before

    when: 'a dry run writes nothing anyway'
    Result dryRun = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then:
    dryRun.exitCode() == 0
    dryRun.out().contains('+++ b/.tib/state.yaml\n')
  }

  def "with --allow-dirty, a service with uncommitted changes is written, with a warning"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    committed()
    (1..12).each { Files.writeString(dir.resolve("File${it}.txt"), 'x\n') }
    git('add', '.')
    git('commit', '-q', '-m', 'files')
    (1..12).each { Files.writeString(dir.resolve("File${it}.txt"), 'y\n') }

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--allow-dirty')

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')
    result.err().readLines() == [
      "${dir.toAbsolutePath().normalize()} has changes that are not committed:".toString(),
      *(['1', '10', '11', '12', '2', '3', '4', '5', '6', '7'].collect {
        "   M File${it}.txt".toString()
      }),
      '  and 2 more',
      'tib writes anyway, as --allow-dirty asks.'
    ]
  }

  def "untracked files, and a run that changes nothing, are no reason to refuse"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    committed()
    Files.writeString(dir.resolve('notes.txt'), 'untracked\n')

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')

    when: 'the files of tib are not committed, and the run changes nothing'
    Result again = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
  }

  /** Makes the service a git repository with everything in it committed. */
  private void committed() {
    git('init', '-q')
    git('add', '.')
    git('commit', '-q', '-m', 'service')
  }

  /** Runs git in the service, without the signing or the identity of the machine. */
  private void git(String... args) {
    Process process = (['git', '-c', 'user.name=tib', '-c', 'user.email=tib@example.com', '-c', 'commit.gpgsign=false'] + args.toList())
    .execute(null, dir.toFile())
    assert process.waitFor() == 0 : process.errorStream.text
  }

  /** The state file of the service. */
  private State state() {
    State.parse(Files.readString(dir.resolve(State.PATH)), '1.2.4')
  }

  /** Every file of the service, with its content, by path; git's own files are not the service's. */
  private Map<String, String> files() {
    Files.walk(dir).withCloseable { paths ->
      paths.filter {
        Files.isRegularFile(it) && !dir.relativize(it).startsWith('.git')
      }.toList().collectEntries {
        [dir.relativize(it).toString(), Files.readString(it)]
      }
    }
  }
}
