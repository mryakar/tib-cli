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
    Generation.run(project, [], pack, { generator }, '1.2.3', tree)

    then:
    tree.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'Service.java']
    tree.changes()*.owner() == [Owner.GENERATOR, Owner.GENERATOR, Owner.DEVELOPER]

    and:
    State state = State.in(tree).get()
    state.tib() == '1.2.3'
    state.pack() == new State.PackRef('p', '1')
    state.inputs() == []
    state.spec() == project.spec()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()*.owner() == [Owner.GENERATOR, Owner.DEVELOPER]
    state.files()[0] == State.File.of('Generated.java', Owner.GENERATOR, 'class Generated {}\n')
  }

  def "a later run keeps the files of the earlier ones in the state"() {
    given:
    Tree first = Tree.at(dir)
    Generation.run(project, [], pack, { generator }, '1.2.3', first)
    first.apply()
    Files.writeString(dir.resolve('Service.java'), 'class Service { int edited; }\n')

    when:
    Tree second = Tree.at(dir)
    Generation.run(project, [], pack, Pack::entity, '1.2.4', second)

    then: 'the developer file is not staged again, but stays on record as it was written'
    second.changes()*.path() == ['.tib/state.yaml', 'test.txt']
    State state = State.in(second).get()
    state.tib() == '1.2.4'
    state.files()*.path() == ['Generated.java', 'Service.java', 'test.txt']
    state.files()[1] == State.File.of('Service.java', Owner.DEVELOPER, 'class Service {}\n')
  }

  def "a file of the developer's that was removed after a run does not come back"() {
    given:
    Tree first = Tree.at(dir)
    Generation.run(project, [], pack, { generator }, '1.2.3', first)
    first.apply()
    Files.delete(dir.resolve('Service.java'))

    when:
    Tree second = Tree.at(dir)
    Generation.run(project, [], pack, { generator }, '1.2.4', second)

    then: 'it stays on record as it was written'
    second.changes()*.path() == ['.tib/state.yaml']
    State state = State.in(second).get()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()[1] == State.File.of('Service.java', Owner.DEVELOPER, 'class Service {}\n')
  }

  def "the inputs of a run take the place of those of the same path, and the others stay"() {
    given:
    State.Input spec = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'a')
    State.Input ddl = new State.Input(State.Input.Kind.DDL, 'db/one.sql', 'b')
    Tree first = Tree.at(dir)
    Generation.run(project, [spec, ddl], pack, { generator }, '1.2.3', first)
    first.apply()

    when:
    State.Input changed = new State.Input(State.Input.Kind.SPEC, 'spec.yaml', 'c')
    State.Input other = new State.Input(State.Input.Kind.DDL, '../db/two.sql', 'd')
    Tree second = Tree.at(dir)
    Generation.run(project, [other, changed], pack, { generator }, '1.2.3', second)

    then:
    State.in(second).get().inputs() == [changed, ddl, other]
  }
}
