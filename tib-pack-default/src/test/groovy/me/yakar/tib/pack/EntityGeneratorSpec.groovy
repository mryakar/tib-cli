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
    existing.each { path ->
      Path file = dir.resolve(EntityGenerator.MIGRATIONS).resolve(path)
      Files.createDirectories(file.parent)
      Files.writeString(file, '-- mine\n')
    }
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
    Path file = dir.resolve(EntityGenerator.MIGRATIONS).resolve('V2__create_a.sql')
    Files.createDirectories(file.parent)
    Files.writeString(file, '-- mine\n')
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n')), tree)

    then:
    tree.changes()*.path().every { !it.endsWith('.sql') }
  }
}
