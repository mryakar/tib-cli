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

import java.nio.file.Path
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class TemplatesSpec extends Specification {

  @TempDir
  Path dir

  def "a header is a comment of the file's kind: #path"() {
    expect:
    Templates.comment(path, 'Mine.') == comment

    where:
    path                     | comment
    'src/main/java/A.java'   | '// Mine.'
    'build.gradle.kts'       | '// Mine.'
    'application.yaml'       | '# Mine.'
    '.gitignore'             | '# Mine.'
    '.gitattributes'         | '# Mine.'
    'db/migration/V1__a.sql' | '-- Mine.'
    'AGENTS.md'              | '<!-- Mine. -->'
  }

  def "a file of a kind without comments gets no header"() {
    when:
    Templates.comment('logo.png', 'Mine.')

    then:
    IllegalArgumentException e = thrown()
    e.message == 'there is no comment in logo.png'
  }

  def "Java is formatted as google-java-format lays it out, with its imports sorted and the unused ones left out"() {
    expect:
    Templates.format('A.java', '''\
package a;
import java.util.Set;
import java.util.List;
class A {   List<String> names() { return List.of(); } }
''') == '''\
package a;

import java.util.List;

class A {
  List<String> names() {
    return List.of();
  }
}
'''
  }

  def "a file that is not Java is left as it is"() {
    expect:
    Templates.format('application.yaml', 'a:   b\n') == 'a:   b\n'
  }

  def "Java that the formatter cannot read is a mistake of the pack, named by its path"() {
    when:
    Templates.format('A.java', 'class {')

    then:
    IllegalStateException e = thrown()
    e.message == 'A.java is not Java that google-java-format reads'
  }

  def "a file imports no class with the name of a class of the service that it names: #file"() {
    given: 'entities with the names of classes that the templates import, and one that refers to them'
    Tree tree = Tree.at(dir)
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
      '''.stripIndent())), tree)

    when:
    List<String> imports = tree.read("src/$file").orElseThrow().readLines().findAll {
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
