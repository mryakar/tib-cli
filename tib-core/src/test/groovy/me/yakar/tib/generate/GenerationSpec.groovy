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
package me.yakar.tib.generate

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.spec.Spec
import me.yakar.tib.state.State
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.ProjectFiles
import me.yakar.tib.tree.Tree
import me.yakar.tib.tree.TreeException
import spock.lang.Specification
import spock.lang.TempDir

class GenerationSpec extends Specification {

  @TempDir
  Path dir

  Pack pack = new TestPack('p', '1')
  Project project = new Project(new Spec(1, 's', null, 'p', null))
  Project one = new Project(new Spec(1, 'one', null, 'p', null))
  Project two = new Project(new Spec(1, 'two', null, 'p', null))

  Generator generator = { Project p, ProjectFiles files ->
    files.own('Generated.java', 'class Generated {}\n')
    files.once('Service.java', 'class Service {}\n')
  } as Generator

  /** Writes the files of a second entity only for the spec of service two. */
  Generator entities = { Project p, ProjectFiles files ->
    files.own('a/AGenerated.java', 'class AGenerated {}\n')
    files.once('a/A.java', 'class A {}\n')
    files.lasting('V1__a.sql', 'a\n')
    if (p.name() == 'two') {
      files.own('b/BGenerated.java', 'class BGenerated {}\n')
      files.once('b/B.java', 'class B {}\n')
      files.once('b/BService.java', 'class BService {}\n')
      files.once('b/BTests.java', 'class BTests {}\n')
      files.lasting('V2__b.sql', 'b\n')
    }
  } as Generator

  def "the first run stages the files of the generator and the state file"() {
    when:
    Map ran = run(project, generator)

    then:
    ran.result == new Generation.Result([], [], [], [], [])
    ran.tree.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'Service.java']
    ran.tree.changes()*.owner() == [Owner.GENERATOR, Owner.GENERATOR, Owner.DEVELOPER]

    and:
    State state = State.in(ran.tree, '1.2.3').get()
    state.tib() == '1.2.3'
    state.pack() == new State.PackRef('p', '1', 'Test 1')
    state.inputs() == []
    state.spec() == project.spec()
    state.files() == [
      State.File.of('Generated.java', Owner.GENERATOR, 'class Generated {}\n'),
      State.File.of('Service.java', Owner.DEVELOPER, 'class Service {}\n')
    ]
  }

  def "the state of a later run follows the previous one, and the files of the generator's that it did not generate again are deleted"() {
    given:
    State.Input spec = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'a')
    Files.writeString(dir.resolve('spec.yaml'), 'a')
    run(project, generator, [spec]).tree.apply(State.PATH)

    when: 'another generator runs'
    Map ran = run(project, pack.entity(), [], '1.2.4')

    then: "the file of the developer's that this generator does not write stays"
    ran.tree.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'test.txt']
    ran.tree.changes()[1].deletes()
    State state = State.in(ran.tree, '1.2.4').get()
    state.tib() == '1.2.4'
    state.inputs() == [spec]
    state.files()*.path() == ['Service.java', 'test.txt']
    ran.result == new Generation.Result([], [], [], [], [])

    when:
    ran.tree.apply(State.PATH)

    then:
    !Files.exists(dir.resolve('Generated.java'))
    Files.exists(dir.resolve('Service.java'))
  }

  def "a run that cannot delete a file leaves the state as it was, and the next run goes on from there"() {
    given:
    run(project, generator).tree.apply(State.PATH)
    Map ran = run(project, pack.entity())

    and: 'the file to delete is a directory with a file in it by the time it is deleted'
    Files.delete(dir.resolve('Generated.java'))
    Files.createDirectories(dir.resolve('Generated.java'))
    Files.writeString(dir.resolve('Generated.java/x'), 'x')

    when:
    ran.tree.apply(State.PATH)

    then:
    thrown(TreeException)
    State.in(Tree.at(dir), '1.2.3').get().files()*.path() == ['Generated.java', 'Service.java']

    when: 'what was in the way is gone'
    dir.resolve('Generated.java').toFile().deleteDir()
    run(project, pack.entity()).tree.apply(State.PATH)

    then: 'the file that the failed run wrote is on record too'
    State.in(Tree.at(dir), '1.2.3').get().files()*.path() == ['Service.java', 'test.txt']

    and:
    run(project, pack.entity()).tree.changes() == []
  }

  def "a file that is there as the generator makes it is on record, and one that is there otherwise is not"() {
    given: 'the files of a run whose state file was not written'
    Files.writeString(dir.resolve('Generated.java'), 'class Generated {}\n')
    Files.writeString(dir.resolve('Service.java'), 'class Service {}\n')
    Files.createDirectories(dir.resolve('a'))
    Files.writeString(dir.resolve('a/A.java'), 'class A { int mine; }\n')
    Files.writeString(dir.resolve('V1__a.sql'), 'a\n')

    when:
    Map ran = run(one, { Project p, ProjectFiles files ->
      generator.generate(p, files)
      entities.generate(p, files)
    } as Generator)

    then:
    ran.tree.changes()*.path() == ['.tib/state.yaml', 'a/AGenerated.java']
    State.in(ran.tree, '1.2.3').get().files() == [
      State.File.of('Generated.java', Owner.GENERATOR, 'class Generated {}\n'),
      State.File.of('Service.java', Owner.DEVELOPER, 'class Service {}\n'),
      State.File.of('V1__a.sql', Owner.DEVELOPER, 'a\n'),
      State.File.of('a/AGenerated.java', Owner.GENERATOR, 'class AGenerated {}\n')
    ]
  }

  def "the files that the spec no longer generates are deleted, but for those that were edited, that last, or that another generator wrote"() {
    given:
    Generator init = { Project p, ProjectFiles files ->
      files.once('Init.java', 'class Init {}\n')
    } as Generator
    run(two, init).tree.apply(State.PATH)
    run(two, entities).tree.apply(State.PATH)

    and: 'the files of the second entity, edited, removed or as they were written'
    Files.writeString(dir.resolve('b/B.java'), 'class B { int mine; }\n')
    Files.writeString(dir.resolve('b/BGenerated.java'), 'class BGenerated { int mine; }\n')
    Files.delete(dir.resolve('b/BTests.java'))

    when: 'the second entity leaves the spec'
    Map ran = run(one, entities)

    then: 'the file that was edited stays, and leaves the record'
    ran.result.kept() == ['b/B.java']
    ran.tree.changes()*.path() == ['.tib/state.yaml', 'b/BGenerated.java', 'b/BService.java']
    ran.tree.changes()*.deletes() == [false, true, true]
    State.in(ran.tree, '1.2.3').get().files()*.path() == [
      'Init.java',
      'V1__a.sql',
      'V2__b.sql',
      'a/A.java',
      'a/AGenerated.java'
    ]

    when:
    ran.tree.apply(State.PATH)

    then:
    Files.exists(dir.resolve('b/B.java'))
    !Files.exists(dir.resolve('b/BService.java'))
    Files.exists(dir.resolve('V2__b.sql'))

    when: 'again'
    ran = run(one, entities)

    then: 'the file that stayed is named once'
    ran.result.kept() == []
    ran.tree.changes() == []
  }

  def "an entity that comes back gets the files that tib has no record of, and the others stay as they are"() {
    given: 'the second entity left the spec, and two of its files stayed, as they were edited'
    run(two, entities).tree.apply(State.PATH)
    Files.writeString(dir.resolve('b/B.java'), 'class B { int mine; }\n')
    Files.writeString(dir.resolve('b/BService.java'), 'class BService { int mine; }\n')
    run(one, entities).tree.apply(State.PATH)

    and: 'one of them was deleted since'
    Files.delete(dir.resolve('b/BService.java'))

    when:
    Map ran = run(two, entities)

    then: 'the file that is there is left alone, and the one that is gone is written'
    ran.tree.changes()*.path() == ['.tib/state.yaml', 'b/BGenerated.java', 'b/BService.java', 'b/BTests.java']
    ran.tree.read('b/BService.java') == Optional.of('class BService {}\n')
    ran.tree.read('b/B.java') == Optional.of('class B { int mine; }\n')

    and: 'the file that lasts kept its record, so it is not written again'
    State.in(ran.tree, '1.2.3').get().files()*.path().contains('V2__b.sql')
  }

  def "the files that last and that the spec generates otherwise, or no longer, are named, and left as they are"() {
    given:
    Generator lasting = { Project p, ProjectFiles files ->
      files.lasting('V1.sql', p.name() == 'two' ? 'a two\n' : 'a\n')
      files.lasting('./V0.sql', '0\n')
      if (p.name() == 'two') {
        files.lasting('V2.sql', 'b\n')
      }
    } as Generator
    run(two, lasting).tree.apply(State.PATH)

    when:
    Map ran = run(one, lasting)

    then: 'from what the previous spec generated to what this one generates'
    ran.result.lastingChanged()*.path() == ['V1.sql']
    ran.result.lastingChanged()[0].diff() == '''\
--- a/V1.sql
+++ b/V1.sql
@@ -1 +1 @@
-a two
+a
'''
    ran.result.lastingDropped() == ['V2.sql']

    and: 'nothing of them is staged'
    ran.tree.changes()*.path() == ['.tib/state.yaml']
  }

  def "the files that last and that the spec generates again are named when tib does not write them"() {
    given: 'a file that is there before tib runs'
    Generator lasting = { Project p, ProjectFiles files ->
      files.lasting('V1.sql', 'a\n')
      if (p.name() == 'two') {
        files.lasting('V2.sql', 'b\n')
        files.lasting('V3.sql', 'c\n')
        files.lasting('V4.sql', 'd\n')
      }
    } as Generator
    Files.writeString(dir.resolve('V4.sql'), 'mine\n')
    run(one, lasting).tree.apply(State.PATH)

    when: 'the spec generates the files for the first time'
    Map ran = run(two, lasting)

    then: 'the ones that the run writes are not named'
    ran.result.lastingReturned()*.path() == ['V4.sql']
    ran.tree.changes()*.path() == ['.tib/state.yaml', 'V2.sql', 'V3.sql']

    when: 'the spec no longer generates them, one is deleted, and the spec generates them again'
    ran.tree.apply(State.PATH)
    run(one, lasting).tree.apply(State.PATH)
    Files.delete(dir.resolve('V3.sql'))
    ran = run(two, lasting)

    then: 'from the file as it is, if it is there, to what the spec generates'
    ran.result.lastingReturned()*.path() == ['V2.sql', 'V3.sql', 'V4.sql']
    ran.result.lastingReturned()*.diff() == [
      '',
      '--- /dev/null\n+++ b/V3.sql\n@@ -0,0 +1 @@\n+c\n',
      '--- a/V4.sql\n+++ b/V4.sql\n@@ -1 +1 @@\n-mine\n+d\n'
    ]
    ran.result.lastingChanged() == []
    ran.result.lastingDropped() == []

    and: 'none of them is written'
    ran.tree.changes()*.path() == ['.tib/state.yaml']

    when: 'again'
    ran.tree.apply(State.PATH)

    then: 'they are named once'
    run(two, lasting).result == new Generation.Result([], [], [], [], [])
  }

  def "a service that was generated for #stack is refused, before anything is staged"() {
    given:
    run(project, generator).tree.apply(State.PATH)
    Path state = dir.resolve(State.PATH)
    Files.writeString(state, Files.readString(state).replace('  stack: Test 1\n', line))
    Tree tree = Tree.at(dir)

    when:
    Generation.next(State.of(tree, '1.2.3'), project, [], new TestPack('p', '2'), generator, '1.2.4', tree)

    then:
    PackException e = thrown()
    e.message == "${tree.root()} was generated for $stack by tib 1.2.3; pack p 2 generates for Test 2\n" +
        'Go on with tib 1.2.3 for this service, or move it to Test 2 by hand.'
    tree.changes() == []

    where:
    stack              | line
    'an earlier stack' | ''
    'Test 1'           | '  stack: Test 1\n'
  }

  def "the inputs that the state forgot are named"() {
    given:
    State.Input gone = new State.Input(State.Input.Kind.DDL, 'db/gone.sql', 'a')
    run(project, generator, [gone]).tree.apply(State.PATH)

    when:
    Map ran = run(project, generator)

    then:
    ran.result.forgotten() == ['db/gone.sql']
    State.in(ran.tree, '1.2.3').get().inputs() == []
  }

  def "a file of the developer's that was removed after a run does not come back"() {
    given:
    run(project, generator).tree.apply(State.PATH)
    Files.delete(dir.resolve('Service.java'))

    when:
    Map ran = run(project, generator, [], '1.2.4')

    then: 'it stays on record as it was written'
    ran.tree.changes()*.path() == ['.tib/state.yaml']
    State state = State.in(ran.tree, '1.2.4').get()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()[1] == State.File.of('Service.java', Owner.DEVELOPER, 'class Service {}\n')
  }

  def "a generator sees the files of the project and the ones it wrote, and what tib wrote before"() {
    given:
    Files.writeString(dir.resolve('there.txt'), 'there\n')
    run(two, entities).tree.apply(State.PATH)
    List<Object> seen = []
    Generator reader = { Project p, ProjectFiles files ->
      files.once('lib/new.bin', [1, 2] as byte[], false)
      seen << files.read('there.txt') << files.exists('b/B.java') << files.exists('none')
      seen << files.list('lib') << files.recorded('a')
    } as Generator

    when:
    run(two, reader)

    then: 'the previous spec is generated too, in a tree of its own'
    seen[0..4] == [Optional.of('there\n'), true, false, ['lib/new.bin'], ['a/A.java', 'a/AGenerated.java']]
    seen[5..9] == seen[0..4]
  }

  /**
   * Runs the generator on the project in the directory, as the first run when tib has no state
   * there and as a later one otherwise, and applies nothing.
   *
   * @return the tree of the run, and its result
   */
  private Map run(Project project, Generator generator, List<State.Input> inputs = [], String version = '1.2.3') {
    Tree tree = Tree.at(dir)
    Optional<State> previous = State.in(tree, version)
    [
      tree  : tree,
      result: previous.present
      ? Generation.next(previous.get(), project, inputs, pack, generator, version, tree)
      : Generation.first(project, inputs, pack, generator, version, tree)
    ]
  }
}
