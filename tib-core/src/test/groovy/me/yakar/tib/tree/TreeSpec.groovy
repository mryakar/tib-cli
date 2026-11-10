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

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermissions
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

class TreeSpec extends Specification {

  /** The path of a file that records a run, which the trees of most tests do not stage. */
  static final String LAST = '.record'

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
    tree.changes()*.before().collect {
      it == null ? null : new String(it, 'UTF-8')
    } == [null, null, '[versions]\n']
    tree.read('Service.java') == Optional.of('class Service {}\n')
    tree.read('libs.versions.toml') == Optional.of('[versions]\nspring = "3.5.0"\n')
  }

  def "lists the files under a directory at any depth, on disk and staged"() {
    given:
    Files.createDirectories(project.resolve('db/migration/old'))
    Files.writeString(project.resolve('db/migration/V1__a.sql'), '')
    Files.writeString(project.resolve('db/migration/old/V0__z.sql'), '')
    Tree tree = Tree.at(project)
    tree.own('db/migration/V2__b.sql', '')
    tree.own('db/migration/deeper/V3__c.sql', '')
    tree.own('db/other.sql', '')

    expect:
    tree.list('db/migration') == [
      'db/migration/V1__a.sql',
      'db/migration/V2__b.sql',
      'db/migration/deeper/V3__c.sql',
      'db/migration/old/V0__z.sql'
    ]
    tree.list('db/none') == []
    tree.list('db').last() == 'db/other.sql'
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

    when:
    tree.apply(LAST)

    then:
    Files.readString(project.resolve('a.txt')) == 'b\n'
    Files.readString(project.resolve('src/main/java/B.java')) == 'class B {}\n'
    tree.changes().empty
    tree.diff().empty
    project.toFile().list().length == 2
  }

  def "a file that is not text cannot be read"() {
    given:
    Files.write(project.resolve('f'), [0xff, 0xfe] as byte[])

    when:
    Tree.at(project).read('f')

    then:
    TreeException e = thrown()
    e.message == "cannot read ${project.resolve('f')}: not UTF-8 text"
  }

  def "a file whose directory cannot be made fails the apply"() {
    given: 'the directory of the new file is a file'
    Files.writeString(project.resolve('f'), 'x')
    Tree tree = Tree.at(project)
    tree.own('f/child', 'x')

    when:
    tree.apply(LAST)

    then:
    TreeException e = thrown()
    e.message == "cannot write ${project.resolve('f')}: already exists"
  }

  def "a file that cannot replace what is there fails the apply, and leaves nothing of its own"() {
    given: 'the second file is a directory with a file in it'
    Files.createDirectories(project.resolve('b/c'))
    Files.writeString(project.resolve('b/c/d'), 'd')
    Tree tree = Tree.at(project)
    tree.own('a/x', 'x')
    tree.own('b/c', 'c')

    when:
    tree.apply(LAST)

    then:
    TreeException e = thrown()
    e.message.startsWith("cannot write ${project.resolve('b/c')}: ")

    and: 'no file was written next to its target, and the change stays staged'
    Files.walk(project).withCloseable { paths ->
      paths.noneMatch {
        it.fileName.toString().endsWith('.tib.tmp')
      }
    }
    Files.readString(project.resolve('b/c/d')) == 'd'
    tree.changes()*.path() == ['a/x', 'b/c']
  }

  def "a file is there without being read"() {
    given:
    Files.write(project.resolve('f'), [0xff, 0xfe] as byte[])
    Tree tree = Tree.at(project)

    when:
    tree.once('f', 'text\n')

    then:
    tree.exists('f')
    tree.changes().empty
  }

  def "a file of the developer's that tib wrote before is not staged again, even when it is gone"() {
    given:
    Tree tree = Tree.at(project)
    tree.writtenBefore(['./src/Service.java', 'Generated.java'])

    when:
    tree.once('src/Service.java', 'class Service {}\n')
    tree.once('src/Controller.java', 'class Controller {}\n')
    tree.own('Generated.java', 'class Generated {}\n')

    then:
    tree.changes()*.path() == ['Generated.java', 'src/Controller.java']
  }

  def "a file that lasts is staged once, as a file of the developer's"() {
    given:
    Files.writeString(project.resolve('There.sql'), 'there\n')
    Tree tree = Tree.at(project)
    tree.writtenBefore(['Before.sql'])

    when:
    tree.lasting('V1.sql', 'v1\n')
    tree.lasting('There.sql', 'other\n')
    tree.lasting('Before.sql', 'before\n')

    then:
    tree.changes()*.path() == ['V1.sql']
    tree.changes()*.owner() == [Owner.DEVELOPER]
  }

  def "a file that is not text is staged as bytes, and cannot be read as text"() {
    given:
    byte[] jar = [0xCA, 0xFE, 0xBA, 0xBE] as byte[]
    Tree tree = Tree.at(project)

    when:
    tree.once('lib/new.jar', jar, false)

    then:
    tree.bytes('lib/new.jar').get() == jar
    tree.diff() == 'Binary files /dev/null and b/lib/new.jar differ\n'

    when:
    tree.read('lib/new.jar')

    then:
    TreeException e = thrown()
    e.message == "cannot read ${project.resolve('lib/new.jar')}: not UTF-8 text"
  }

  def "a file that cannot be read fails, as a directory does"() {
    when:
    Tree.read(project)

    then:
    TreeException e = thrown()
    e.message.startsWith("cannot read ${project}")
  }

  def "a file to run is written as one, where the file system can say so"() {
    given:
    Tree tree = Tree.at(project)
    tree.once('gradlew', '#!/bin/sh\n'.getBytes('UTF-8'), true)
    tree.once('gradlew.bat', '@echo off\r\n'.getBytes('UTF-8'), false)

    when:
    tree.apply(LAST)

    then:
    Files.readString(project.resolve('gradlew')) == '#!/bin/sh\n'
    Files.isExecutable(project.resolve('gradlew'))
    Files.readString(project.resolve('gradlew.bat')) == '@echo off\r\n'

    and: 'the other one is not, where the file system tells the two apart'
    !FileSystems.default.supportedFileAttributeViews().contains('posix') || !Files.isExecutable(project.resolve('gradlew.bat'))
  }

  def "a deleted file is gone from what the run sees"() {
    given:
    Files.writeString(project.resolve('a.txt'), 'a\n')
    Files.createDirectories(project.resolve('src'))
    Files.writeString(project.resolve('src/B.java'), 'class B {}\n')
    Tree tree = Tree.at(project)

    when:
    tree.delete('./a.txt', Owner.GENERATOR)
    tree.delete('src/B.java', Owner.DEVELOPER)
    tree.own('gone.txt', 'staged\n')
    tree.delete('gone.txt', Owner.DEVELOPER)
    tree.own('src/C.java', 'class C {}\n')
    tree.own('other/D.java', 'class D {}\n')

    then:
    !tree.exists('a.txt')
    tree.read('a.txt').empty
    tree.list('src') == ['src/C.java']

    and: 'a file that is not there has no change'
    tree.changes()*.path() == ['a.txt', 'other/D.java', 'src/B.java', 'src/C.java']
    tree.changes()*.deletes() == [true, false, true, false]
    with(tree.changes()[0]) {
      path() == 'a.txt'
      new String(before(), 'UTF-8') == 'a\n'
      after() == null
      owner() == Owner.GENERATOR
      !executable()
    }
  }

  def "applying deletes the files, and the directories that this leaves empty, up to the root"() {
    given:
    ['top.txt', 'x/y/z.txt', 'src/main/B.java', 'src/C.java'].each {
      Files.createDirectories(project.resolve(it).parent)
      Files.writeString(project.resolve(it), it)
    }
    Tree tree = Tree.at(project)
    tree.delete('top.txt', Owner.GENERATOR)
    tree.delete('x/y/z.txt', Owner.GENERATOR)
    tree.delete('src/main/B.java', Owner.DEVELOPER)
    tree.own('n.txt', 'n\n')

    when:
    tree.apply(LAST)

    then:
    project.toFile().list() as Set == ['n.txt', 'src'] as Set
    project.resolve('src').toFile().list() == ['C.java'] as String[]
    tree.changes().empty
  }

  def "a file that cannot be deleted fails the apply"() {
    given: 'the file is a directory with a file in it by the time it is deleted'
    Files.writeString(project.resolve('d'), 'd')
    Tree tree = Tree.at(project)
    tree.delete('d', Owner.GENERATOR)
    Files.delete(project.resolve('d'))
    Files.createDirectories(project.resolve('d'))
    Files.writeString(project.resolve('d/e'), 'e')

    when:
    tree.apply(LAST)

    then:
    TreeException e = thrown()
    e.message.startsWith("cannot delete ${project.resolve('d')}: ")
    tree.changes()*.path() == ['d']
  }

  def "the file at the last path is written after the others and after the deletions"() {
    given:
    Files.writeString(project.resolve('old.txt'), 'old\n')
    Tree tree = Tree.at(project)
    tree.own('.record', 'new\n')
    tree.own('a.txt', 'a\n')
    tree.delete('old.txt', Owner.GENERATOR)

    when:
    tree.apply('./.record')

    then:
    project.toFile().list() as Set == ['.record', 'a.txt'] as Set
    Files.readString(project.resolve('.record')) == 'new\n'
    tree.changes().empty

    when: 'the last path is not staged'
    tree.own('a.txt', 'b\n')
    tree.apply('.record')

    then:
    Files.readString(project.resolve('a.txt')) == 'b\n'
    Files.readString(project.resolve('.record')) == 'new\n'
  }

  def "a file that cannot be #verb leaves the file at the last path as it was"() {
    given: 'a file that sorts first, as the one that records the run does'
    Files.writeString(project.resolve('.record'), 'old\n')
    Files.writeString(project.resolve('d'), 'd')
    Tree tree = Tree.at(project)
    tree.own('.record', 'new\n')
    stage(tree)

    and: 'the other file is a directory with a file in it by the time it is written or deleted'
    Files.delete(project.resolve('d'))
    Files.createDirectories(project.resolve('d'))
    Files.writeString(project.resolve('d/e'), 'e')

    when:
    tree.apply('.record')

    then:
    TreeException e = thrown()
    e.message.startsWith("cannot $verb ${project.resolve('d')}: ")
    Files.readString(project.resolve('.record')) == 'old\n'
    project.toFile().list() as Set == ['.record', 'd'] as Set

    where:
    verb     | stage
    'write'  | { it.own('d', 'other') }
    'delete' | { it.delete('d', Owner.GENERATOR) }
  }

  def "lists the files under a directory that tib wrote before, there or not, in order"() {
    given:
    Files.createDirectories(project.resolve('db/migration'))
    Files.writeString(project.resolve('db/migration/V3__c.sql'), '')
    Tree tree = Tree.at(project)
    tree.writtenBefore([
      'db/migration/V2__b.sql',
      './db/migration/deeper/V1__a.sql',
      'db/other.sql',
      'db/migration/V1__a.sql'
    ])

    expect:
    tree.recorded('db/migration') == [
      'db/migration/V1__a.sql',
      'db/migration/V2__b.sql',
      'db/migration/deeper/V1__a.sql'
    ]
    tree.recorded('db/none') == []
  }

  @Requires({ FileSystems.default.supportedFileAttributeViews().contains('posix') })
  def "a new file gets the permissions of any new file, and a replaced file keeps its own"() {
    given:
    Files.writeString(project.resolve('run.sh'), 'echo a\n')
    Files.setPosixFilePermissions(project.resolve('run.sh'), PosixFilePermissions.fromString('rwxr-xr-x'))
    Tree tree = Tree.at(project)
    tree.own('run.sh', 'echo b\n')
    tree.own('src/A.java', 'class A {}\n')

    when:
    tree.apply(LAST)

    then:
    Files.readString(project.resolve('run.sh')) == 'echo b\n'
    PosixFilePermissions.toString(Files.getPosixFilePermissions(project.resolve('run.sh'))) == 'rwxr-xr-x'
    Files.getPosixFilePermissions(project.resolve('src/A.java')) == Files.getPosixFilePermissions(Files.createFile(project.resolve('src/new')))
  }

  def "a file written next to its target gets the attributes of the target, when it is there"() {
    given:
    FileTime time = FileTime.fromMillis(86_400_000)
    Path file = Files.writeString(project.resolve('a.txt'), 'a\n')
    Files.setLastModifiedTime(file, time)
    Path staging = Files.writeString(project.resolve('a.txt.tib.tmp'), 'b\n')
    Path other = Files.writeString(project.resolve('b.txt.tib.tmp'), 'b\n')
    FileTime otherTime = Files.getLastModifiedTime(other)

    when:
    Tree.keep(file, staging, ['basic:lastModifiedTime'])
    Tree.keep(project.resolve('b.txt'), other, ['basic:lastModifiedTime'])

    then:
    Files.getLastModifiedTime(staging) == time
    Files.getLastModifiedTime(other) == otherTime
  }

  def "a file written next to its target before is replaced"() {
    given:
    Files.writeString(project.resolve('a.txt.tib.tmp'), 'left\n')
    Tree tree = Tree.at(project)
    tree.own('a.txt', 'a\n')

    when:
    tree.apply(LAST)

    then:
    Files.readString(project.resolve('a.txt')) == 'a\n'
    Files.notExists(project.resolve('a.txt.tib.tmp'))
  }
}
