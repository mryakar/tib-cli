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
package me.yakar.tib.pack.plain

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.generate.Generation
import me.yakar.tib.generate.Project
import me.yakar.tib.pack.Migration
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.state.State
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class EntityGeneratorSpec extends Specification {

  /** A service with one entity. */
  static final Project A = project('entities:\n  - name: A\n')

  static final String BASE = 'src/main/java/com/example/s/'

  @TempDir
  Path dir

  def "an entity gets its classes, its migration, and the tests of the kinds of the spec: #tests"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("tests: $tests\nentities:\n  - name: A\n  - name: B\n    options: { dto: false }\n"), tree)
    List<String> written = tree.changes()*.path().findAll {
      it.startsWith('src/test/')
    }.collect {
      it.substring(it.lastIndexOf('/') + 1) - '.java'
    }

    then:
    written.toSorted() == expected.toSorted()
    tree.changes()*.path().findAll {
      it.startsWith('src/main/java/com/example/s/a/')
    }.collect {
      it - 'src/main/java/com/example/s/a/' - '.java'
    }.toSorted() ==
    ['A', 'ADto', 'AGenerated', 'AHandler', 'AMapper', 'ARepository', 'AService']
    tree.changes()*.path().findAll {
      it.startsWith('src/main/java/com/example/s/b/')
    }.collect {
      it - 'src/main/java/com/example/s/b/' - '.java'
    }.toSorted() ==
    ['B', 'BGenerated', 'BRepository']

    where:
    tests                      | expected
    '[unit, integration, api]' | ['AApiTests', 'ARepositoryTests', 'ASamples', 'AServiceTests', 'BRepositoryTests', 'BSamples']
    '[integration]'            | ['ARepositoryTests', 'ASamples', 'BRepositoryTests', 'BSamples']
    '[unit, api]'              | ['AApiTests', 'ASamples', 'AServiceTests']
    '[unit]'                   | ['ASamples', 'AServiceTests']
    '[api]'                    | ['AApiTests', 'ASamples']
    '[]'                       | []
  }

  def "the files of an entity have their owners: the ones that follow the spec are tib's"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(A, tree)
    Map<String, Owner> owners = tree.changes().collectEntries {
      [it.path() - 'src/main/java/com/example/s/' - 'src/test/java/com/example/s/', it.owner()]
    }

    then:
    [
      'a/AGenerated.java',
      'a/ARepository.java',
      'a/ADto.java',
      'a/AMapper.java',
      'a/ASamples.java',
      'Routes.java'
    ].every {
      owners[it] == Owner.GENERATOR
    }
    [
      'a/A.java',
      'a/AService.java',
      'a/AHandler.java',
      'a/AServiceTests.java',
      'a/ARepositoryTests.java',
      'a/AApiTests.java'
    ].every {
      owners[it] == Owner.DEVELOPER
    }
  }

  def "the routes wire the entities with a service and a handler, and only those: #entities"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("entities:\n$entities"), tree)
    String routes = tree.read(BASE + 'Routes.java').orElseThrow()

    then:
    routes.contains('new AHandler(') == a
    routes.contains('new BHandler(') == b
    routes.contains('getFilters().add(new ErrorHandler())') == (a || b)

    where:
    entities                                                            | a     | b
    '  - name: A\n  - name: B\n'                                        | true  | true
    '  - name: A\n  - name: B\n    options: { mapper: false }\n'        | true  | false
    '  - name: A\n    options: { dto: false }\n'                        | false | false
  }

  def "the first entity with an API brings the support of the API, and the first one in pages the page, once: #entities"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("entities:\n$entities"), tree)
    List<String> paths = tree.changes()*.path()

    then:
    ['NotFoundException', 'BadRequestException', 'ErrorHandler', 'Http'].every {
      paths.contains("${BASE}${it}.java" as String) == api
    }
    paths.contains("${BASE}Page.java" as String) == page
    tree.changes().findAll {
      it.path().startsWith(BASE) && !(it.path() - BASE).contains('/') && !it.path().endsWith('Routes.java')
    }.every {
      it.owner() == Owner.DEVELOPER
    }

    where:
    entities                                                                    | api   | page
    '  - name: A\n'                                                             | true  | true
    '  - name: A\n    options: { pagination: false }\n'                         | true  | false
    '  - name: A\n    options: { mapper: false }\n'                             | false | true
    '  - name: A\n    options: { mapper: false, pagination: false }\n'          | false | false
  }

  def "the support of the API is written once: there #there, written before #recorded"() {
    given:
    String handler = BASE + 'ErrorHandler.java'
    if (there) {
      Files.createDirectories(dir.resolve(handler).parent)
      Files.writeString(dir.resolve(handler), 'mine\n')
    }
    Tree tree = Tree.at(dir)
    if (recorded) {
      tree.writtenBefore([handler])
    }

    when:
    new EntityGenerator().generate(A, tree)

    then:
    !tree.changes()*.path().contains(handler)

    where:
    there | recorded
    true  | false
    false | true
    true  | true
  }

  def "the migration of a table is the one of the default pack, and lasts: a run whose spec no longer has the table does not delete it"() {
    given:
    PlainPack pack = new PlainPack()
    Tree tree = Tree.at(dir)
    new Generation(pack, '0.0.0', tree).first(A, [])
    new Generation(pack, '0.0.0', tree).next(State.of(tree, '0.0.0'), A, [])
    tree.apply(State.PATH)

    when:
    Tree later = Tree.at(dir)
    new Generation(pack, '0.0.0', later).next(State.of(later, '0.0.0'), project(''), [])

    then:
    Files.readString(dir.resolve("${Migration.DIRECTORY}/V1__create_a.sql")).endsWith('CREATE TABLE A (\n  ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY\n);\n')
    List<String> deleted = later.changes().findAll { it.deletes() }*.path()
    deleted.contains(BASE + 'a/A.java')
    !deleted.contains("${Migration.DIRECTORY}/V1__create_a.sql" as String)
    later.read(BASE + 'Routes.java').orElseThrow().contains('public static void add(HttpServer server, Transactions transactions) {}\n')
  }

  private static Project project(String yaml) {
    new Project(SpecReader.parse("tib: 1\nservice: s\npack: plain\n$yaml"))
  }
}
