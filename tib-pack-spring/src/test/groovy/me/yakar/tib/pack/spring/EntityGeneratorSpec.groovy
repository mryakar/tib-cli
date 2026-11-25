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
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class EntityGeneratorSpec extends Specification {

  /** A service with one entity. */
  static final Project A = new Project(SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: A\n'))

  static final String HANDLER = 'src/main/java/com/example/s/ConflictHandler.java'

  @TempDir
  Path dir

  @Shared
  @TempDir
  Path clashing

  /** A service whose entities have the names of classes that the templates import. */
  @Shared
  Tree clashes

  def setupSpec() {
    clashes = Tree.at(clashing)
    new EntityGenerator().generate(new Project(SpecReader.parse('''\
      tib: 1
      service: s
      entities:
        - name: Instant
          table: INSTANTS
        - name: UUID
          table: UUIDS
          fields: [{ name: label, type: String, nullable: false }]
        - name: Size
          table: SIZES
          fields: [{ name: label, type: String }]
        - name: Valid
          table: VALIDS
        - name: BeforeEach
          table: BEFORE
        - name: Holder
          audit: true
          relations:
            - { kind: manyToOne, target: Instant, nullable: false }
            - { kind: manyToOne, target: UUID }
      '''.stripIndent())), clashes)
  }

  def "a file imports no class with the name of a class of the service that it names: #file"() {
    when:
    List<String> imports = clashes.read("src/$file").orElseThrow().readLines().findAll {
      it.startsWith('import ') && it.endsWith(".$name;")
    }

    then:
    imports == imported

    where:
    file                                                        | name         | imported
    'main/java/com/example/s/instant/InstantGenerated.java'     | 'Instant'    | []
    'test/java/com/example/s/instant/InstantSamples.java'       | 'Instant'    | []
    'main/java/com/example/s/uuid/UUIDGenerated.java'           | 'UUID'       | []
    'main/java/com/example/s/uuid/UUIDRepository.java'          | 'UUID'       | []
    'main/java/com/example/s/uuid/UUIDService.java'             | 'UUID'       | []
    'test/java/com/example/s/uuid/UUIDServiceTests.java'        | 'UUID'       | []
    'test/java/com/example/s/beforeeach/BeforeEachRepositoryTests.java' | 'BeforeEach' | []
    'main/java/com/example/s/holder/HolderGenerated.java'       | 'UUID'       | ['import com.example.s.uuid.UUID;']
    'test/java/com/example/s/holder/HolderSamples.java'         | 'Instant'    | ['import com.example.s.instant.Instant;']
    'test/java/com/example/s/holder/HolderApiTests.java'        | 'Instant'    | ['import com.example.s.instant.Instant;']
    'main/java/com/example/s/uuid/UUIDDto.java'                 | 'Size'       | ['import jakarta.validation.constraints.Size;']
    'main/java/com/example/s/size/SizeDto.java'                 | 'Size'       | ['import jakarta.validation.constraints.Size;']
    'main/java/com/example/s/valid/ValidController.java'        | 'Valid'      | ['import jakarta.validation.Valid;']
  }

  def "a list and a page of entities come in the order of their identifiers, which the tests of the API show: #architecture"() {
    given:
    Tree tree = Tree.at(dir)
    Project project = new Project(SpecReader.parse("tib: 1\nservice: s\narchitecture: $architecture\nentities:\n  - name: A\n    fields: [{ name: label, type: String }]\n  - name: B\n    id: { type: UUID }\n    options: { pagination: false }\n  - name: C\n    fields: [{ name: code, type: String, unique: true }]\n"))

    when:
    new EntityGenerator().generate(project, tree)
    String main = 'src/main/java/com/example/s/'
    String tests = 'src/test/java/com/example/s/'

    then: 'a page is sorted by the identifier unless the request asks for another order'
    tree.read(main + "a/${web}AController.java").orElseThrow().contains('list(@SortDefault("id") Pageable pageable) {\n')
    !tree.read(main + "b/${web}BController.java").orElseThrow().contains('Sort')

    and: 'a list is sorted where it is read'
    tree.read(main + list).orElseThrow().contains('findAll(org.springframework.data.domain.Sort.by("id"))')
    !tree.read(main + list.replace('b/', 'a/').replace('/B', '/A')).orElseThrow().contains('Sort')

    and: 'the tests of the API change the row that comes first and expect it first, among the entities of the answer alone; identifiers that are UUIDs sort as their text'
    tree.read(tests + "a/${web}AApiTests.java").orElseThrow().contains('    http.put("/api/as/{id}", other, first.id());\n')
    tree.read(tests + "a/${web}AApiTests.java").orElseThrow().contains('        .containsExactly(first.id().toString(), second.id().toString());\n')
    tree.read(tests + "a/${web}AApiTests.java").orElseThrow().contains('    JsonNode listed = list.getBody().path("content");\n')
    tree.read(tests + "b/${web}BApiTests.java").orElseThrow().contains('    http.put("/api/bs/{id}", other, firstId.compareTo(secondId) < 0 ? firstId : secondId);\n')
    tree.read(tests + "b/${web}BApiTests.java").orElseThrow().contains('    JsonNode listed = list.getBody();\n')
    tree.read(tests + "b/${web}BApiTests.java").orElseThrow().contains('    assertThat(listed.findValuesAsString("id")).hasSize(2).isSorted();\n')
    !tree.read(tests + "c/${web}CApiTests.java").orElseThrow().contains('InTheOrderOfTheirIdentifiers')

    where:
    architecture | web               | list
    'anemic'     | ''                | 'b/BService.java'
    'ddd'        | 'web/'            | 'b/application/BService.java'
    'hexagonal'  | 'adapter/in/web/' | 'b/adapter/out/persistence/BPersistenceAdapter.java'
  }

  def "in #architecture, a file imports the class of the service that it names, and no other class of its name: #file"() {
    given:
    Tree tree = Tree.at(dir)
    String entities = 'entities:\n  - name: Instant\n    table: INSTANTS\n  - name: UUID\n    table: UUIDS\n    fields: [{ name: label, type: String, nullable: false }]\n'

    when:
    new EntityGenerator().generate(new Project(SpecReader.parse("tib: 1\nservice: s\narchitecture: $architecture\n$entities")), tree)
    List<String> imports = tree.read("src/$file").orElseThrow().readLines().findAll {
      it.startsWith('import ') && it.endsWith(".$name;")
    }

    then:
    imports == imported

    where:
    architecture | file                                                                       | name      | imported
    'ddd'        | 'main/java/com/example/s/instant/domain/InstantGenerated.java'             | 'Instant' | []
    'ddd'        | 'main/java/com/example/s/instant/domain/InstantRepository.java'            | 'Instant' | []
    'ddd'        | 'main/java/com/example/s/instant/web/InstantDto.java'                      | 'Instant' | []
    'ddd'        | 'main/java/com/example/s/instant/application/InstantService.java'          | 'Instant' | ['import com.example.s.instant.domain.Instant;']
    'ddd'        | 'test/java/com/example/s/instant/InstantSamples.java'                      | 'Instant' | ['import com.example.s.instant.domain.Instant;']
    'ddd'        | 'main/java/com/example/s/uuid/domain/UUIDGenerated.java'                   | 'UUID'    | []
    'ddd'        | 'main/java/com/example/s/uuid/web/UUIDDto.java'                            | 'UUID'    | []
    'ddd'        | 'main/java/com/example/s/uuid/application/UUIDService.java'                | 'UUID'    | ['import com.example.s.uuid.domain.UUID;']
    'ddd'        | 'test/java/com/example/s/uuid/application/UUIDServiceTests.java'           | 'UUID'    | ['import com.example.s.uuid.domain.UUID;']
    'hexagonal'  | 'main/java/com/example/s/instant/domain/InstantGenerated.java'             | 'Instant' | []
    'hexagonal'  | 'main/java/com/example/s/instant/adapter/out/persistence/InstantEntity.java' | 'Instant' | []
    'hexagonal'  | 'main/java/com/example/s/instant/adapter/in/web/InstantDto.java'           | 'Instant' | []
    'hexagonal'  | 'main/java/com/example/s/instant/adapter/out/persistence/InstantPersistenceAdapter.java' | 'Instant' | ['import com.example.s.instant.domain.Instant;']
    'hexagonal'  | 'main/java/com/example/s/instant/application/InstantService.java'          | 'Instant' | ['import com.example.s.instant.domain.Instant;']
    'hexagonal'  | 'main/java/com/example/s/uuid/domain/UUIDGenerated.java'                   | 'UUID'    | []
    'hexagonal'  | 'main/java/com/example/s/uuid/adapter/out/persistence/UUIDEntity.java'     | 'UUID'    | []
    'hexagonal'  | 'main/java/com/example/s/uuid/adapter/out/persistence/UUIDPersistenceAdapter.java' | 'UUID' | ['import com.example.s.uuid.domain.UUID;']
    'hexagonal'  | 'test/java/com/example/s/uuid/application/UUIDServiceTests.java'           | 'UUID'    | ['import com.example.s.uuid.domain.UUID;']
  }

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
    SpringPack pack = new SpringPack()
    Tree tree = Tree.at(dir)
    new Generation(pack, '0.0.0', tree).first(A, [])
    new Generation(pack, '0.0.0', tree).next(State.of(tree, '0.0.0'), A, [])
    tree.apply(State.PATH)

    when:
    Tree later = Tree.at(dir)
    new Generation(pack, '0.0.0', later).next(State.of(later, '0.0.0'), new Project(SpecReader.parse('tib: 1\nservice: s\n')), [])

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
