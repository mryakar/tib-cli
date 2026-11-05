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
import me.yakar.tib.spec.Names
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class StateSpec extends Specification {

  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))

  @TempDir
  Path dir

  /** A state whose spec sets every value, so that every key of the file is in it. */
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
    State.parse(snapshot) == state
  }

  def "the state of a project is in its tree, if tib ran on it before"() {
    given:
    Tree tree = Tree.at(dir)

    expect:
    State.in(tree).empty

    when:
    tree.own(State.PATH, state.yaml())

    then:
    State.in(tree) == Optional.of(state)
  }

  def "a state without inputs or files has none"() {
    expect:
    State state = State.parse('spec:\n  tib: 1\n  service: s\n')
    state.inputs() == []
    state.files() == []
  }

  def "a state that tib cannot read is refused: '#yaml'"() {
    when:
    State.parse(yaml)

    then:
    StateException e = thrown()
    e.message.startsWith(message)

    where:
    yaml                                  | message
    '['                                   | '.tib/state.yaml: '
    'tib: [1]\nspec:\n  tib: 1\n  service: s' | '.tib/state.yaml: '
    'other: 1\n'                          | '.tib/state.yaml: '
    ''                                    | '.tib/state.yaml: '
    '---\n'                                | '.tib/state.yaml: spec: required'
    'files: []\n'                         | '.tib/state.yaml: spec: required'
  }

  def "the spec in the state is checked as a spec, and its problem is one of the state file"() {
    when:
    State.parse('spec:\n  tib: 1\n  service: Vehicle Service\n')

    then:
    StateException e = thrown()
    e.message == ".tib/state.yaml: the spec has a problem: service: 'Vehicle Service' does not match ${Names.SERVICE}"
  }

  def "the state of a tree without one says that it is not a tib project"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    State.of(tree)

    then:
    StateException e = thrown()
    e.message == "${tree.root()} is not a tib project: there is no .tib/state.yaml"

    when:
    tree.own(State.PATH, state.yaml())

    then:
    State.of(tree) == state
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
    Difference.Kind.values()*.toString() == ['edited', 'missing', 'removed']
  }

  def "an input is kept with its path relative to the service and the hash of its content"() {
    given:
    Path service = Files.createDirectories(dir.resolve('services/vehicle'))
    Path db = Files.createDirectories(dir.resolve('db'))
    Files.writeString(service.resolve('spec.yaml'), '')
    Files.writeString(db.resolve('vehicle.sql'), 'CREATE TABLE V (ID BIGINT);\n')

    expect:
    State.Input.of(State.Input.Kind.SPEC, service, service.resolve('spec.yaml')) ==
        new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855')
    State.Input.of(State.Input.Kind.DDL, service, service.resolve('../../db/./vehicle.sql')).path() == '../../db/vehicle.sql'
    State.Input.of(State.Input.Kind.DDL, service, db.resolve('vehicle.sql')).sha256() ==
        State.sha256('CREATE TABLE V (ID BIGINT);\n')
  }

  def "a path without the root of the service keeps its whole path"() {
    expect: 'a relative path stands for one on another root, as on another drive, which only some systems have'
    State.Input.path(dir, Path.of('db', 'vehicle.sql')) == 'db/vehicle.sql'
  }

  def "an input that cannot be read cannot be kept"() {
    when:
    State.Input.of(State.Input.Kind.SPEC, dir, dir.resolve('missing.yaml'))

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
}
