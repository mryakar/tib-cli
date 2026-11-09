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
package me.yakar.tib.pack

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class EntityGeneratorSpec extends Specification {

  @TempDir
  Path dir

  def "an entity comes after the entities it refers to, otherwise in the order of the spec"() {
    given:
    Project project = new Project(SpecReader.parse('''\
tib: 1
service: s
entities:
  - name: A
    relations:
      - { kind: manyToOne, target: B }
  - name: B
    relations:
      - { kind: manyToOne, target: B, field: parent }
  - name: C
    relations:
      - { kind: manyToOne, target: B }
      - { kind: manyToOne, target: B, field: other }
'''))

    expect:
    EntityGenerator.ordered(project.spec().entities())*.name() == ['B', 'A', 'C']
  }

  def "the migration of a new table comes after the migrations there: #existing"() {
    given:
    existing.each { writeMigration(it) }
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n')), tree)

    then:
    tree.changes()*.path().find {
      it.endsWith('.sql')
    } == "${EntityGenerator.MIGRATIONS}/${migration}"

    where:
    existing                                       | migration
    []                                             | 'V1__create_a.sql'
    ['V3__by_hand.sql', 'R__views.sql']            | 'V4__create_a.sql'
    ['V202611011200__by_hand.sql']                 | 'V202611011201__create_a.sql'
    ['V2.1__by_hand.sql']                          | 'V3__create_a.sql'
    ['V2_1_3__by_hand.sql', 'V1__by_hand.sql']     | 'V3__create_a.sql'
    ['V1__by_hand.sql', 'older/V5__by_hand.sql']   | 'V6__create_a.sql'
    ['notes.txt']                                  | 'V1__create_a.sql'
  }

  def "the migration of a table that has one is not written again"() {
    given:
    writeMigration('V2__create_a.sql')
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n')), tree)

    then:
    tree.changes()*.path().every { !it.endsWith('.sql') }
  }

  def "the migration of a table that tib wrote before is not written again, also when it is #how"() {
    given:
    onDisk.each { writeMigration(it) }
    Tree tree = Tree.at(dir)
    tree.writtenBefore(['V0__create_b.sql', 'V1__create_a.sql'].collect {
      "${EntityGenerator.MIGRATIONS}/${it}".toString()
    })

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n')), tree)

    then:
    tree.changes()*.path().every { !it.endsWith('.sql') }

    where:
    how       | onDisk
    'gone'    | ['V2__x.sql']
    'renamed' | ['V1__a.sql']
  }

  def "the first entity with an API brings the handler of conflicts, once: #entities"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse("tib: 1\nservice: s\nentities:\n$entities")), tree)

    then:
    tree.changes()*.path().contains('src/main/java/com/example/s/ConflictHandler.java') == handler

    where:
    entities                                                          | handler
    '  - name: A\n'                                                   | true
    '  - name: A\n    options: { mapper: false }\n'                    | false
    '  - name: A\n    options: { dto: false, mapper: false }\n  - name: B\n' | true
  }

  def "the handler of conflicts is written once: one that is there, or that tib wrote before, stays"() {
    given:
    Path handler = dir.resolve('src/main/java/com/example/s/ConflictHandler.java')
    if (there) {
      Files.createDirectories(handler.parent)
      Files.writeString(handler, 'mine\n')
    }
    Tree tree = Tree.at(dir)
    tree.writtenBefore(['src/main/java/com/example/s/ConflictHandler.java'])

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n')), tree)

    then:
    !tree.changes()*.path().contains('src/main/java/com/example/s/ConflictHandler.java')

    where:
    there << [true, false]
  }

  /** A migration of the developer's at the path, under the migrations of the service. */
  private void writeMigration(String path) {
    Path file = dir.resolve(EntityGenerator.MIGRATIONS).resolve(path)
    Files.createDirectories(file.parent)
    Files.writeString(file, '-- mine\n')
  }
}
