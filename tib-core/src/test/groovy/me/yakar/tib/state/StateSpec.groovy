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
import me.yakar.tib.spec.SpecException
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
          options: { dto: false, mapper: false, pagination: false, events: true }
      '''.stripIndent()),
  [
    new State.File('AGENTS.md', Owner.GENERATOR, 'abc'),
    new State.File('build.gradle.kts', Owner.DEVELOPER, 'def'),
    new State.File('gradle/libs.versions.toml', Owner.SHARED, '012')
  ])

  def "the state file changes only on purpose, and tib reads it back"() {
    given:
    String snapshot = Files.readString(SNAPSHOTS.resolve('state.yaml'))
    Path actual = Path.of('build', 'snapshots', 'state.yaml')
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

  def "a state without files has none"() {
    expect:
    State.parse('spec:\n  tib: 1\n  service: s\n').files() == []
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

  def "the spec in the state is checked as a spec"() {
    when:
    State.parse('spec:\n  tib: 1\n  service: Vehicle Service\n')

    then:
    SpecException e = thrown()
    e.problems() == ["service: 'Vehicle Service' does not match ${Names.SERVICE}"]
  }
}
