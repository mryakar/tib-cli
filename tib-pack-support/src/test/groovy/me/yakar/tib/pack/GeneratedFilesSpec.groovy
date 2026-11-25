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
import me.yakar.tib.tree.Change
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class GeneratedFilesSpec extends Specification {

  static final Project PROJECT = new Project(SpecReader.parse('tib: 1\nservice: s\n'))

  static final String BASE = 'src/main/java/com/example/s/Base.java'

  @TempDir
  Path dir

  Tree tree

  GeneratedFiles files

  def setup() {
    Files.writeString(dir.resolve('There.java'), 'mine\n')
    Files.writeString(dir.resolve('there.yaml'), 'mine\n')
    tree = Tree.at(dir)
    files = new GeneratedFiles(new Templates('gg.jte.generated.support'), tree)
  }

  def "a file of tib's is written on every run, under the header that says so"() {
    when:
    files.own('A.java', 'class_java.jte', [name: 'A'])
    files.own('There.java', 'class_java.jte', [name: 'There'])

    then:
    tree.changes()*.path() == ['A.java', 'There.java']
    tree.changes().every { it.owner() == Owner.GENERATOR }
    text(tree.changes()[0]) == '// Written by tib on every run. Do not edit: your changes would be lost.\nclass A {\n  void a() {}\n}\n'
  }

  def "a file of the developer's is written once, under the header that says when: #header"() {
    when:
    write(files, 'a.yaml')
    write(files, 'there.yaml')

    then:
    tree.changes()*.path() == ['a.yaml']
    tree.changes()[0].owner() == Owner.DEVELOPER
    text(tree.changes()[0]) == "# $header\nname:   A\n"

    where:
    header                                                                    | write
    'Written by tib when the service was created; it is yours from then on.' | { GeneratedFiles files, String path ->
      files.onceWithService(path, 'class_yaml.jte', [name: 'A'])
    }
    'Written by tib when the entity was created; it is yours from then on.'  | { GeneratedFiles files, String path ->
      files.onceWithEntity(path, 'class_yaml.jte', [name: 'A'])
    }
  }

  def "a class that the service needs once it has entities is written in the base package, from the project"() {
    when:
    files.onceWithFirstEntity(PROJECT, 'Base', 'base_java.jte')

    then:
    tree.changes()*.path() == [BASE]
    tree.changes()[0].owner() == Owner.DEVELOPER
    text(tree.changes()[0]) == '// Written by tib with the first entity of the service; it is yours from then on.\npackage com.example.s;\n\nclass Base {}\n'
  }

  def "a class that tib wrote with an earlier entity is not written again"() {
    given:
    tree.writtenBefore([BASE])

    when:
    files.onceWithFirstEntity(PROJECT, 'Base', 'base_java.jte')

    then:
    tree.changes() == []
  }

  private static String text(Change change) {
    new String(change.after(), 'UTF-8')
  }
}
