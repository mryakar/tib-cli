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
package me.yakar.tib.tree

import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Specification
import spock.lang.TempDir

class TreeSpec extends Specification {

  @TempDir
  Path project

  def "reads what is on disk and what is staged on top of it"() {
    given:
    Files.writeString(project.resolve('build.gradle'), 'plugins {}\n')
    Tree tree = Tree.at(project)

    expect:
    tree.root() == project.toAbsolutePath().normalize()
    tree.read('build.gradle') == Optional.of('plugins {}\n')
    tree.read('settings.gradle').empty
    !tree.exists('settings.gradle')

    when:
    tree.own('settings.gradle', 'rootProject.name = "s"\n')

    then:
    tree.exists('settings.gradle')
    tree.read('./settings.gradle') == Optional.of('rootProject.name = "s"\n')
    Files.notExists(project.resolve('settings.gradle'))
  }

  def "stages the three kinds of ownership"() {
    given:
    Files.writeString(project.resolve('Service.java'), 'class Service {}\n')
    Files.writeString(project.resolve('libs.versions.toml'), '[versions]\n')
    Tree tree = Tree.at(project)

    when:
    tree.own('Generated.java', 'class Generated {}\n')
    tree.once('Service.java', 'class Service extends Generated {}\n')
    tree.once('Controller.java', 'class Controller {}\n')
    tree.edit('libs.versions.toml') { it + 'spring = "3.5.0"\n' }

    then:
    tree.changes()*.path() == ['Controller.java', 'Generated.java', 'libs.versions.toml']
    tree.changes()*.owner() == [Owner.DEVELOPER, Owner.GENERATOR, Owner.SHARED]
    tree.changes()*.kind() == [Change.Kind.CREATED, Change.Kind.CREATED, Change.Kind.CHANGED]
    tree.read('Service.java') == Optional.of('class Service {}\n')
    tree.read('libs.versions.toml') == Optional.of('[versions]\nspring = "3.5.0"\n')
  }

  def "a shared file has to be there"() {
    when:
    Tree.at(project).edit('application.yaml') { it }

    then:
    TreeException e = thrown()
    e.message == 'application.yaml is not there to edit'
  }

  def "a path outside the project is refused: #path"() {
    when:
    Tree.at(project).read(path)

    then:
    TreeException e = thrown()
    e.message == "$path is outside the project"

    where:
    path << ['../other.txt', '.', 'a/../../b', '/']
  }

  def "the diff is the diff of every change, and a change back is no change"() {
    given:
    Files.writeString(project.resolve('a.txt'), 'a\n')
    Tree tree = Tree.at(project)

    when:
    tree.own('a.txt', 'b\n')
    tree.own('src/main/java/B.java', 'class B {}\n')

    then:
    tree.diff() == '''\
--- a/a.txt
+++ b/a.txt
@@ -1 +1 @@
-a
+b
--- /dev/null
+++ b/src/main/java/B.java
@@ -0,0 +1 @@
+class B {}
'''

    when:
    tree.own('a.txt', 'a\n')

    then:
    tree.changes()*.path() == ['src/main/java/B.java']
  }

  def "applying writes the files, creates their directories and stages nothing more"() {
    given:
    Files.writeString(project.resolve('a.txt'), 'a\n')
    Tree tree = Tree.at(project)
    tree.own('a.txt', 'b\n')
    tree.own('src/main/java/B.java', 'class B {}\n')
    String diff = tree.diff()

    when:
    tree.apply()

    then:
    Files.readString(project.resolve('a.txt')) == 'b\n'
    Files.readString(project.resolve('src/main/java/B.java')) == 'class B {}\n'
    tree.changes().empty
    tree.diff().empty
    Files.list(project).count() == 2

    and: 'the dry run showed what was applied'
    Tree.at(project).own('a.txt', 'a\n')
    diff.contains('-a\n+b\n')

    and: 'a second run of the same generation changes nothing'
    Tree again = Tree.at(project)
    again.own('a.txt', 'b\n')
    again.own('src/main/java/B.java', 'class B {}\n')
    again.changes().empty
  }

  def "a file that is not text cannot be read"() {
    given:
    Files.write(project.resolve('f'), [0xff, 0xfe] as byte[])

    when:
    Tree.at(project).read('f')

    then:
    thrown(UncheckedIOException)
  }

  def "a file that cannot be written fails the apply"() {
    given: 'the directory of the new file is a file'
    Files.writeString(project.resolve('f'), 'x')
    Tree tree = Tree.at(project)
    tree.own('f/child', 'x')

    when:
    tree.apply()

    then:
    thrown(UncheckedIOException)
  }
}
