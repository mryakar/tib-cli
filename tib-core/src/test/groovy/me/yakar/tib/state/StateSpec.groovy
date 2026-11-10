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
package me.yakar.tib.state

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import me.yakar.tib.spec.Names
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class StateSpec extends Specification {

  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))

  @TempDir
  Path dir

  /** A state whose spec sets every value, so that every key of the file is in it. */
  @Shared
  State state = new State(
  '0.1.0',
  new State.PackRef('default', '0.1.0'),
  [
    new State.Input(State.Input.Kind.SPEC, 'vehicle-service.yaml', '345'),
    new State.Input(State.Input.Kind.DDL, '../db/vehicle.sql', '678')
  ],
  SpecReader.parse('''\
      tib: 1
      service: vehicle-service
      group: me.yakar
      pack: default@0.1.0
      entities:
        - name: Depot
          table: DEPOTS
          id: { field: key, type: UUID, strategy: uuid }
        - name: Vehicle
          id: { type: Long, strategy: sequence }
          fields:
            - { name: plateNumber, column: PLATE, type: String, length: 16, nullable: false, unique: true }
            - { name: price, type: BigDecimal, precision: 12, scale: 2 }
          relations:
            - { kind: manyToOne, target: Depot, field: home, column: HOME_ID, nullable: false }
          audit: true
          options: { dto: false, mapper: false, pagination: false }
      '''.stripIndent()),
  [
    new State.File('AGENTS.md', Owner.GENERATOR, 'abc'),
    new State.File('build.gradle.kts', Owner.DEVELOPER, 'def'),
    new State.File('gradle/libs.versions.toml', Owner.SHARED, '012')
  ])

  def "the state file changes only on purpose, and tib reads it back"() {
    given:
    String snapshot = Files.readString(SNAPSHOTS.resolve('state.yaml'))
    Path actual = Path.of(System.getProperty('tib.snapshots.actual'), 'state.yaml')
    Files.createDirectories(actual.parent)
    Files.writeString(actual, state.yaml())

    expect: "if not, the new state file is in $actual, to be copied over the snapshot on purpose"
    state.yaml() == snapshot
    State.parse(snapshot, '0.1.0') == state
  }

  def "the state file of every released tib is read: #version"() {
    when:
    State released = State.parse(StateSpec.getResource("state-${version}.yaml").text, version)

    then:
    released.tib() == version
    released.inputs() == inputs

    where:
    version | inputs
    '0.1.0' | []
  }

  def "the state of a project is in its tree, if tib ran on it before"() {
    given:
    Tree tree = Tree.at(dir)

    expect:
    State.in(tree, '0.1.0').empty

    when:
    tree.own(State.PATH, state.yaml())

    then:
    State.in(tree, '0.1.0') == Optional.of(state)
  }

  def "a state without inputs or files has none"() {
    expect:
    State state = State.parse('tib: 0.1.0\npack: { name: default, version: 0.1.0 }\nspec:\n  tib: 1\n  service: s\n', '0.1.0')
    state.inputs() == []
    state.files() == []
  }

  def "a state that tib cannot read is refused: '#yaml'"() {
    when:
    State.parse(yaml, '0.1.0')

    then:
    StateException e = thrown()
    e.problems().size() == 1
    e.problems()[0].startsWith(problem)
    e.message == "the state file has a problem: ${e.problems()[0]}"

    where:
    yaml                                            | problem
    "tib: 'unterminated"                            | 'not YAML: '
    '['                                             | 'state: '
    ''                                              | 'state: the file is empty'
    '---\n'                                         | 'state: the file is empty'
    '- tib: 1'                                      | 'state: '
    'tib: [1]\nspec:\n  tib: 1\n  service: s'       | 'tib: '
    'other: 1\n'                                    | 'other: unknown property; the properties are files, inputs, pack, spec, tib'
    'files:\n- { path: a, owner: me, sha256: x }\n' | "files[0].owner: 'me' is not one of generator, developer, shared"
    'spec:\n  tib: 1\n  service: s\n  other: 1\n'   | 'spec.other: unknown property; the properties are entities, group, pack, service, tib'
  }

  def "a state without a value that it needs is refused: #problem"() {
    when:
    State.parse(with(values).yaml(), '0.1.0')

    then:
    StateException e = thrown()
    e.problems() == [problem]
    e.message == "the state file has a problem: $problem"

    where:
    values                                                                   | problem
    [tib: null]                                                              | 'tib: required'
    [pack: null]                                                             | 'pack: required'
    [pack: new State.PackRef(null, '0.1.0')]                                 | 'pack.name: required'
    [pack: new State.PackRef('default', null)]                               | 'pack.version: required'
    [inputs: [state.inputs()[0], new State.Input(null, 'a.sql', '1')]]       | 'inputs[1].kind: required'
    [inputs: [new State.Input(State.Input.Kind.DDL, null, '1')]]             | 'inputs[0].path: required'
    [inputs: [new State.Input(State.Input.Kind.DDL, 'a.sql', null)]]         | 'inputs[0].sha256: required'
    [inputs: [null]]                                                         | 'inputs[0]: missing'
    [spec: null]                                                             | 'spec: required'
    [files: [new State.File(null, Owner.GENERATOR, '1')]]                    | 'files[0].path: required'
    [files: [state.files()[0], new State.File('a', null, '1')]]              | 'files[1].owner: required'
    [files: [new State.File('a', Owner.GENERATOR, null)]]                    | 'files[0].sha256: required'
    [files: [state.files()[0], null]]                                        | 'files[1]: missing'
  }

  def "the spec in the state is checked as a spec, and each of its problems is one of the state file"() {
    when:
    State.parse(with(tib: null, spec: new Spec(1, 'Vehicle Service', 'Me', null, null)).yaml(), '0.1.0')

    then:
    StateException e = thrown()
    e.problems() == [
      'tib: required',
      "spec.service: 'Vehicle Service' does not match ${Names.SERVICE}",
      "spec.group: 'Me' does not match ${Names.GROUP}"
    ]
    e.message == "the state file has 3 problems:\n  ${e.problems().join('\n  ')}"
  }

  def "a state that a newer tib wrote is refused, before what this tib does not know is read"() {
    when:
    State.parse('tib: 0.3.0\npack: { name: default, version: 0.3.0 }\nformat: 2\nspec:\n  tib: 1\n  service: s\n', '0.2.0-SNAPSHOT')

    then:
    StateException e = thrown()
    e.message == '.tib/state.yaml was written by tib 0.3.0, which is newer than this tib 0.2.0-SNAPSHOT; upgrade tib'
    e.problems() == [e.message]
  }

  def "a version of tib is newer than another by its numbers, whatever its suffix: #version than #than"() {
    expect:
    State.newer(version, than) == newer

    where:
    version          | than             | newer
    '0.3.0'          | '0.2.0'          | true
    '0.10.0'         | '0.9.0'          | true
    '1.0.0'          | '0.99.99'        | true
    '0.2.1'          | '0.2.0-SNAPSHOT' | true
    '0.2.0'          | '0.2.0-SNAPSHOT' | false
    '0.2.0-SNAPSHOT' | '0.2.0'          | false
    '0.1.0'          | '0.2.0'          | false
    'docs'           | '0.2.0'          | false
    '0.3.0'          | 'docs'           | false
  }

  def "a first state has the inputs of its run and the files that it staged"() {
    given:
    Tree tree = Tree.at(dir)
    tree.own('A.java', 'class A {}\n')
    tree.once('B.java', 'class B {}\n')

    when:
    State next = State.next(Optional.empty(), '1.2.3', state.pack(), state.inputs(), state.spec(), tree)

    then:
    next == new State('1.2.3', state.pack(), state.inputs(), state.spec(), [
      State.File.of('A.java', Owner.GENERATOR, 'class A {}\n'),
      State.File.of('B.java', Owner.DEVELOPER, 'class B {}\n')
    ])
  }

  def "the inputs of a run take the place of those of the same path, and the others stay"() {
    given:
    State.Input spec = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'a')
    State.Input ddl = new State.Input(State.Input.Kind.DDL, 'db/one.sql', 'b')
    State.Input changed = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'c')
    State.Input other = new State.Input(State.Input.Kind.DDL, '../db/two.sql', 'd')

    when:
    State next = State.next(Optional.of(with(inputs: [spec, ddl])), '1.2.3', state.pack(), [other, changed], state.spec(), Tree.at(dir))

    then:
    next.inputs() == [changed, ddl, other]
  }

  def "the files that a run staged take the place of their records, and the other records stay"() {
    given:
    State previous = with(files: [
      State.File.of('Mine.java', Owner.DEVELOPER, 'class Mine {}\n'),
      State.File.of('Gen.java', Owner.GENERATOR, 'class Gen {}\n'),
      State.File.of('libs.toml', Owner.SHARED, '[versions]\n')
    ])
    Tree tree = Tree.at(dir)
    tree.writtenBefore(previous.files()*.path())
    tree.own('Gen.java', 'class Gen { int x; }\n')
    tree.once('Mine.java', 'class Mine { int x; }\n')
    tree.own('New.java', 'class New {}\n')

    when:
    State next = State.next(Optional.of(previous), '1.2.4', state.pack(), [], state.spec(), tree)

    then: "a file of the developer's that tib wrote before is not staged again, and keeps its record"
    next.tib() == '1.2.4'
    next.files() == [
      State.File.of('Gen.java', Owner.GENERATOR, 'class Gen { int x; }\n'),
      previous.files()[0],
      State.File.of('New.java', Owner.GENERATOR, 'class New {}\n'),
      previous.files()[2]
    ]
  }

  def "the files that a run deleted take their records with them, there or not"() {
    given: 'a state with the files of two entities, and a run that deletes those of the second'
    State previous = with(files: [
      State.File.of('a/A.java', Owner.DEVELOPER, 'class A {}\n'),
      State.File.of('a/AGenerated.java', Owner.GENERATOR, 'class AGenerated {}\n'),
      State.File.of('b/B.java', Owner.DEVELOPER, 'class B {}\n'),
      State.File.of('b/BGenerated.java', Owner.GENERATOR, 'class BGenerated {}\n'),
      State.File.of('libs.toml', Owner.SHARED, '[versions]\n')
    ])
    Files.createDirectories(dir.resolve('b'))
    Files.writeString(dir.resolve('b/BGenerated.java'), 'class BGenerated {}\n')
    Tree tree = Tree.at(dir)
    tree.writtenBefore(previous.files()*.path())
    tree.delete('b/B.java', Owner.DEVELOPER)
    tree.delete('b/BGenerated.java', Owner.GENERATOR)

    when:
    State next = State.next(Optional.of(previous), '1.2.4', state.pack(), [], state.spec(), tree)

    then: 'only the file that is there is a change'
    tree.changes()*.path() == ['b/BGenerated.java']
    next.files() == previous.files() - previous.files()[2..3]
  }

  def "the state of a tree without one says that it is not a tib project"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    State.of(tree, '0.1.0')

    then:
    StateException e = thrown()
    e.message == "${tree.root()} is not a tib project: there is no .tib/state.yaml"

    when:
    tree.own(State.PATH, state.yaml())

    then:
    State.of(tree, '0.1.0') == state
  }

  def "the differences are the files of tib that were edited or are missing, and the files of the developer that were removed"() {
    given:
    Map<String, Owner> owners = [
      'Edited.java'    : Owner.GENERATOR,
      'Kept.java'      : Owner.GENERATOR,
      'Missing.java'   : Owner.GENERATOR,
      'Mine.java'      : Owner.DEVELOPER,
      'Removed.java'   : Owner.DEVELOPER,
      'Shared.toml'    : Owner.SHARED,
      'SharedGone.toml': Owner.SHARED
    ]
    State written = new State('0.1.0', state.pack(), [], state.spec(), owners.collect { path, owner ->
      State.File.of(path, owner, "$path\n")
    })
    owners.keySet().each { Files.writeString(dir.resolve(it), "$it\n") }
    ['Edited.java', 'Mine.java', 'Shared.toml'].each {
      Files.writeString(dir.resolve(it), 'edited\n')
    }
    ['Missing.java', 'Removed.java', 'SharedGone.toml'].each { Files.delete(dir.resolve(it)) }

    expect:
    written.differences(Tree.at(dir)) == [
      new Difference('Edited.java', Difference.Kind.EDITED),
      new Difference('Missing.java', Difference.Kind.MISSING),
      new Difference('Removed.java', Difference.Kind.REMOVED),
      new Difference('SharedGone.toml', Difference.Kind.MISSING)
    ]
  }

  def "a state whose files are as tib wrote them has no differences"() {
    given:
    State written = new State('0.1.0', state.pack(), [], state.spec(), [State.File.of('A.java', Owner.GENERATOR, 'class A {}\n')])
    Files.writeString(dir.resolve('A.java'), 'class A {}\n')

    expect:
    written.differences(Tree.at(dir)) == []
  }

  def "a file of the developer's is not read, so one that is not UTF-8 is no difference"() {
    given:
    State written = new State('0.1.0', state.pack(), [], state.spec(), [State.File.of('V2__depot.sql', Owner.DEVELOPER, '-- Şube\n')])
    Files.write(dir.resolve('V2__depot.sql'), '-- Şube\n'.getBytes('windows-1254'))

    expect:
    written.differences(Tree.at(dir)) == []
  }

  def "a kind of difference is named in lower case"() {
    expect:
    Difference.Kind.values()*.toString() == ['changed', 'absent', 'edited', 'missing', 'removed']
  }

  def "the input differences are the inputs that changed or are not there, in the order of the state"() {
    given:
    Path service = Files.createDirectories(dir.resolve('service'))
    Files.createDirectories(dir.resolve('db'))
    State read = inputs(service,
        Files.writeString(service.resolve('kept.yaml'), 'kept\n'),
        Files.writeString(service.resolve('changed.yaml'), 'read\n'),
        Files.writeString(dir.resolve('db/changed.sql'), 'read\n'),
        Files.writeString(service.resolve('absent.yaml'), 'read\n'))
    Files.writeString(service.resolve('changed.yaml'), 'changed\n')
    Files.writeString(dir.resolve('db/changed.sql'), 'changed\n')
    Files.delete(service.resolve('absent.yaml'))

    expect:
    read.inputDifferences(service) == [
      new Difference('changed.yaml', Difference.Kind.CHANGED),
      new Difference('../db/changed.sql', Difference.Kind.CHANGED),
      new Difference('absent.yaml', Difference.Kind.ABSENT)
    ]
  }

  def "a state whose inputs are as tib read them, or that has none, has no input differences"() {
    given:
    Path spec = Files.writeString(dir.resolve('spec.yaml'), 'spec\n')

    expect:
    inputs(dir, spec).inputDifferences(dir) == []
    withInputs([]).inputDifferences(dir) == []
  }

  def "an input is hashed by its bytes, so one that is no longer UTF-8 is a changed one"() {
    given:
    Path ddl = Files.writeString(dir.resolve('vehicle.sql'), '-- Şube\n')
    State read = inputs(dir, ddl)
    Files.write(ddl, '-- Şube\n'.getBytes('windows-1254'))

    expect:
    read.inputDifferences(dir) == [new Difference('vehicle.sql', Difference.Kind.CHANGED)]
  }

  def "an input kept with its whole path is read where it is"() {
    given:
    Path spec = Files.writeString(dir.resolve('spec.yaml'), 'spec\n')
    State read = withInputs([
      new State.Input(State.Input.Kind.SPEC, spec.toAbsolutePath().toString(), State.sha256('read\n'))
    ])

    expect:
    read.inputDifferences(Files.createDirectories(dir.resolve('service'))) == [new Difference(spec.toAbsolutePath().toString(), Difference.Kind.CHANGED)]
  }

  def "an input that is there but cannot be read is a problem"() {
    given:
    Path spec = Files.writeString(dir.resolve('spec.yaml'), 'spec\n')
    State read = inputs(dir, spec)
    Files.delete(spec)
    Files.createDirectory(spec)

    when:
    read.inputDifferences(dir)

    then:
    StateException e = thrown()
    e.message.startsWith("cannot read ${spec}: ")
  }

  def "an input is kept with its path relative to the service and the hash of its content"() {
    given:
    Path service = Files.createDirectories(dir.resolve('services/vehicle'))

    expect:
    State.Input.of(State.Input.Kind.SPEC, service, service.resolve('spec.yaml'), '') ==
        new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855')
    State.Input.of(State.Input.Kind.DDL, service, service.resolve('../../db/./vehicle.sql'), '').path() == '../../db/vehicle.sql'
  }

  def "the content of an input is read once, and its hash is the hash of the bytes read"() {
    given:
    Path ddl = dir.resolve('vehicle.sql')
    Files.write(ddl, 'CREATE TABLE ŞUBE (ID BIGINT);\n'.getBytes('UTF-8'))

    when:
    String content = State.Input.read(ddl)

    then:
    content == 'CREATE TABLE ŞUBE (ID BIGINT);\n'
    State.Input.of(State.Input.Kind.DDL, dir, ddl, content).sha256() ==
        MessageDigest.getInstance('SHA-256').digest(Files.readAllBytes(ddl)).encodeHex().toString()
  }

  def "a path without the root of the service keeps its whole path"() {
    expect: 'a relative path stands for one on another root, as on another drive, which only some systems have'
    State.Input.path(dir, Path.of('db', 'vehicle.sql')) == 'db/vehicle.sql'
  }

  def "an input that cannot be read cannot be kept"() {
    when:
    State.Input.read(dir.resolve('missing.yaml'))

    then:
    StateException e = thrown()
    e.message == "cannot read ${dir.resolve('missing.yaml')}: no such file"
  }

  def "a kind of input is named in lower case"() {
    expect:
    State.Input.Kind.values()*.toString() == ['spec', 'ddl']
  }

  def "the hash is the SHA-256 in hex"() {
    expect:
    State.File.of('a', Owner.GENERATOR, '').sha256() == 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
  }

  def "an algorithm that the runtime does not have is an IllegalStateException"() {
    when:
    State.digest('NO-SUCH', new byte[0])

    then:
    thrown(IllegalStateException)
  }

  /** The state with the values of the map in place of its own. */
  private State with(Map<String, Object> values) {
    Map<String, Object> all = [tib: state.tib(), pack: state.pack(), inputs: state.inputs(), spec: state.spec(), files: state.files()] + values
    new State(all.tib as String, all.pack as State.PackRef, all.inputs as List, all.spec as Spec, all.files as List)
  }

  /** The state with the files as its inputs, each as tib reads it from the directory of the service. */
  private State inputs(Path service, Path... files) {
    withInputs(files.collect { Path file ->
      State.Input.of(State.Input.Kind.SPEC, service, file, Files.readString(file))
    })
  }

  /** The state with other inputs. */
  private State withInputs(List<State.Input> inputs) {
    new State(state.tib(), state.pack(), inputs, state.spec(), state.files())
  }
}
