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
    List<String> forgotten = Generation.run(Optional.empty(), project, [], pack, {
      generator
    }, '1.2.3', tree)

    then:
    forgotten == []
    tree.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'Service.java']
    tree.changes()*.owner() == [Owner.GENERATOR, Owner.GENERATOR, Owner.DEVELOPER]

    and:
    State state = State.in(tree, '1.2.3').get()
    state.tib() == '1.2.3'
    state.pack() == new State.PackRef('p', '1')
    state.inputs() == []
    state.spec() == project.spec()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()*.owner() == [Owner.GENERATOR, Owner.DEVELOPER]
    state.files()[0] == State.File.of('Generated.java', Owner.GENERATOR, 'class Generated {}\n')
  }

  def "the state of a later run follows the previous one, and forgets the files of the generator's that it did not stage again"() {
    given:
    State.Input spec = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'a')
    Tree first = Tree.at(dir)
    Generation.run(Optional.empty(), project, [spec], pack, { generator }, '1.2.3', first)
    first.apply()

    when:
    Tree second = Tree.at(dir)
    List<String> forgotten = Generation.run(State.in(second, '1.2.4'), project, [], pack, Pack::entity, '1.2.4', second)

    then:
    second.changes()*.path() == ['.tib/state.yaml', 'test.txt']
    State state = State.in(second, '1.2.4').get()
    state.tib() == '1.2.4'
    state.inputs() == [spec]
    state.files()*.path() == ['Service.java', 'test.txt']
    forgotten == ['Generated.java']
    Files.exists(dir.resolve('Generated.java'))
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
