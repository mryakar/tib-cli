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
package me.yakar.tib.pack.spring

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.generate.Generation
import me.yakar.tib.generate.Project
import me.yakar.tib.pack.Migration
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.state.State
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class EntityGeneratorSpec extends Specification {

  /** A service with one entity. */
  static final Project A = new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n'))

  static final String HANDLER = 'src/main/java/com/example/s/ConflictHandler.java'

  @TempDir
  Path dir

  def "an entity gets the tests of the kinds of the spec: #tests"() {
    given:
    Tree tree = Tree.at(dir)
    Project project = new Project(SpecReader.parse("tib: 1\nservice: s\ntests: $tests\nentities:\n  - name: A\n  - name: B\n    options: { dto: false }\n"))

    when:
    new EntityGenerator().generate(project, tree)
    List<String> written = tree.changes()*.path().findAll {
      it.startsWith('src/test/')
    }.collect {
      it.substring(it.lastIndexOf('/') + 1) - '.java'
    }

    then:
    written.toSorted() == expected.toSorted()

    where:
    tests                      | expected
    '[unit, integration, api]' | ['AApiTests', 'ARepositoryTests', 'ASamples', 'AServiceTests', 'BRepositoryTests', 'BSamples']
    '[integration]'            | ['ARepositoryTests', 'ASamples', 'BRepositoryTests', 'BSamples']
    '[unit, api]'              | ['AApiTests', 'ASamples', 'AServiceTests']
    '[unit]'                   | ['ASamples', 'AServiceTests']
    '[api]'                    | ['AApiTests', 'ASamples']
    '[]'                       | []
  }

  def "in the hexagonal architecture, an entity gets the tests of the kinds of the spec where they test: #tests"() {
    given:
    Tree tree = Tree.at(dir)
    Project project = new Project(SpecReader.parse("tib: 1\nservice: s\narchitecture: hexagonal\ntests: $tests\nentities:\n  - name: A\n  - name: B\n    options: { dto: false }\n"))

    when:
    new EntityGenerator().generate(project, tree)
    List<String> written = tree.changes()*.path().findAll {
      it.startsWith('src/test/')
    }.collect {
      it - 'src/test/java/com/example/s/' - '.java'
    }

    then:
    written.toSorted() == expected.toSorted()

    where:
    tests                      | expected
    '[unit, integration, api]' | [
      'a/adapter/in/web/AApiTests',
      'a/adapter/out/persistence/APersistenceAdapterTests',
      'a/ASamples',
      'a/application/AServiceTests',
      'b/adapter/out/persistence/BPersistenceAdapterTests',
      'b/BSamples'
    ]
    '[unit]'                   | ['a/ASamples', 'a/application/AServiceTests']
  }

  def "the migration of a table lasts: a run whose spec no longer has the table does not delete it"() {
    given:
    DefaultPack pack = new DefaultPack()
    Tree tree = Tree.at(dir)
    Generation.first(A, [], pack, pack.entity(), '0.0.0', tree)
    tree.apply(State.PATH)

    when:
    Tree later = Tree.at(dir)
    Generation.next(State.of(later, '0.0.0'), new Project(SpecReader.parse('tib: 1\nservice: s\n')), [], pack, pack.entity(), '0.0.0', later)

    then:
    List<String> deleted = later.changes().findAll { it.deletes() }*.path()
    deleted.contains('src/main/java/com/example/s/a/A.java')
    !deleted.contains("${Migration.DIRECTORY}/V1__create_a.sql" as String)
  }

  def "the first entity with an API brings the handler of conflicts, once: #entities"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse("tib: 1\nservice: s\nentities:\n$entities")), tree)

    then:
    tree.changes()*.path().contains(HANDLER) == handler

    where:
    entities                                                          | handler
    '  - name: A\n'                                                   | true
    '  - name: A\n    options: { mapper: false }\n'                    | false
    '  - name: A\n    options: { dto: false, mapper: false }\n  - name: B\n' | true
  }

  def "the handler of conflicts is written once: there #there, written before #recorded"() {
    given:
    if (there) {
      Files.createDirectories(dir.resolve(HANDLER).parent)
      Files.writeString(dir.resolve(HANDLER), 'mine\n')
    }
    Tree tree = Tree.at(dir)
    if (recorded) {
      tree.writtenBefore([HANDLER])
    }

    when:
    new EntityGenerator().generate(A, tree)

    then:
    !tree.changes()*.path().contains(HANDLER)

    where:
    there | recorded
    true  | false
    false | true
    true  | true
  }
}
