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

import java.nio.file.Path
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Tree
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class TemplatesSpec extends Specification {

  @Shared
  @TempDir
  Path dir

  /** A service whose entities have the names of classes that the templates import. */
  @Shared
  Tree clashes

  def setupSpec() {
    clashes = Tree.at(dir)
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
}
