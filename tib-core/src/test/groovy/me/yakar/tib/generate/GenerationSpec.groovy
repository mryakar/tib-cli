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
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class GenerationSpec extends Specification {

  @TempDir
  Path dir

  Pack pack = new TestPack('p', '1')
  Project project = new Project(new Spec(1, 's', null, 'p', null))
  Generator generator = { Project p, Tree tree ->
    tree.own('Generated.java', 'class Generated {}\n')
    tree.once('Service.java', 'class Service {}\n')
  } as Generator

  def "stages the files of the generator and the state file"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    Generation.Result result = Generation.run(Optional.empty(), project, [], pack, {
      generator
    }, '1.2.3', tree)

    then:
    result == new Generation.Result([], [])
    tree.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'Service.java']
    tree.changes()*.owner() == [Owner.GENERATOR, Owner.GENERATOR, Owner.DEVELOPER]

    and:
    State state = State.in(tree, '1.2.3').get()
    state.tib() == '1.2.3'
    state.pack() == new State.PackRef('p', '1', 'Test 1')
    state.inputs() == []
    state.spec() == project.spec()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()*.owner() == [Owner.GENERATOR, Owner.DEVELOPER]
    state.files()[0] == State.File.of('Generated.java', Owner.GENERATOR, 'class Generated {}\n')
  }

  def "the state of a later run follows the previous one, and the files of the generator's that it did not stage again are deleted"() {
    given:
    State.Input spec = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'a')
    Files.writeString(dir.resolve('spec.yaml'), 'a')
    Tree first = Tree.at(dir)
    Generation.run(Optional.empty(), project, [spec], pack, { generator }, '1.2.3', first)
    first.apply()

    when:
    Tree second = Tree.at(dir)
    Generation.Result result = Generation.run(State.in(second, '1.2.4'), project, [], pack, Pack::entity, '1.2.4', second)

    then: 'the file of the developer\'s that this generator does not write stays'
    second.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'test.txt']
    second.changes()[1].deletes()
    State state = State.in(second, '1.2.4').get()
    state.tib() == '1.2.4'
    state.inputs() == [spec]
    state.files()*.path() == ['Service.java', 'test.txt']
    result == new Generation.Result([], [])

    when:
    second.apply()

    then:
    !Files.exists(dir.resolve('Generated.java'))
    Files.exists(dir.resolve('Service.java'))
  }

  def "the files that the spec no longer generates are deleted, but for those that were edited, that last, or that another generator wrote"() {
    given: 'a generator that writes the files of a second entity only for the spec of service two'
    Generator entities = { Project p, Tree tree ->
      tree.own('a/AGenerated.java', 'class AGenerated {}\n')
      tree.once('a/A.java', 'class A {}\n')
      tree.lasting('V1__a.sql', 'a\n')
      if (p.name() == 'two') {
        tree.own('b/BGenerated.java', 'class BGenerated {}\n')
        tree.once('b/B.java', 'class B {}\n')
        tree.once('b/BService.java', 'class BService {}\n')
        tree.once('b/BTests.java', 'class BTests {}\n')
        tree.lasting('V2__b.sql', 'b\n')
      }
    } as Generator
    Generator init = { Project p, Tree tree ->
      tree.once('Init.java', 'class Init {}\n')
    } as Generator
    Project one = new Project(new Spec(1, 'one', null, 'p', null))
    Project two = new Project(new Spec(1, 'two', null, 'p', null))
    run(Optional.empty(), two, init)
    run(State.in(Tree.at(dir), '1.2.3'), two, entities)

    and: 'the files of the second entity, edited, removed or as they were written'
    Files.writeString(dir.resolve('b/B.java'), 'class B { int mine; }\n')
    Files.writeString(dir.resolve('b/BGenerated.java'), 'class BGenerated { int mine; }\n')
    Files.delete(dir.resolve('b/BTests.java'))

    when: 'the second entity leaves the spec'
    Tree tree = Tree.at(dir)
    Generation.Result result = Generation.run(State.in(tree, '1.2.3'), one, [], pack, {
      entities
    }, '1.2.3', tree)

    then:
    result.kept() == ['b/B.java']
    tree.changes()*.path() == ['.tib/state.yaml', 'b/BGenerated.java', 'b/BService.java']
    tree.changes()*.deletes() == [false, true, true]
    State.in(tree, '1.2.3').get().files()*.path() == [
      'Init.java',
      'V1__a.sql',
      'V2__b.sql',
      'a/A.java',
      'a/AGenerated.java',
      'b/B.java'
    ]

    when:
    tree.apply()

    then:
    Files.exists(dir.resolve('b/B.java'))
    !Files.exists(dir.resolve('b/BService.java'))
    Files.exists(dir.resolve('V2__b.sql'))

    when: 'again'
    Tree again = Tree.at(dir)
    result = Generation.run(State.in(again, '1.2.3'), one, [], pack, { entities }, '1.2.3', again)

    then: 'the file that stayed is named once'
    result.kept() == []
    again.changes() == []

    when: 'the second entity comes back'
    Tree back = Tree.at(dir)
    Generation.run(State.in(back, '1.2.3'), two, [], pack, { entities }, '1.2.3', back)

    then: 'the files that were deleted are written again, and the one that stayed is left as it is'
    back.changes()*.path() == ['.tib/state.yaml', 'b/BGenerated.java', 'b/BService.java', 'b/BTests.java']
  }

  private void run(Optional<State> previous, Project project, Generator generator, List<State.Input> inputs = []) {
    Tree tree = Tree.at(dir)
    Generation.run(previous, project, inputs, pack, { generator }, '1.2.3', tree)
    tree.apply()
  }

  def "the inputs that the state forgot are named"() {
    given:
    State.Input gone = new State.Input(State.Input.Kind.DDL, 'db/gone.sql', 'a')
    run(Optional.empty(), project, generator, [gone])

    when:
    Tree tree = Tree.at(dir)
    Generation.Result result = Generation.run(State.in(tree, '1.2.3'), project, [], pack, {
      generator
    }, '1.2.3', tree)

    then:
    result.forgotten() == ['db/gone.sql']
    State.in(tree, '1.2.3').get().inputs() == []
  }

  def "a file of the developer's that was removed after a run does not come back"() {
    given:
    Tree first = Tree.at(dir)
    Generation.run(Optional.empty(), project, [], pack, { generator }, '1.2.3', first)
    first.apply()
    Files.delete(dir.resolve('Service.java'))

    when:
    Tree second = Tree.at(dir)
    Generation.run(State.in(second, '1.2.4'), project, [], pack, { generator }, '1.2.4', second)

    then: 'it stays on record as it was written'
    second.changes()*.path() == ['.tib/state.yaml']
    State state = State.in(second, '1.2.4').get()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()[1] == State.File.of('Service.java', Owner.DEVELOPER, 'class Service {}\n')
  }
}
