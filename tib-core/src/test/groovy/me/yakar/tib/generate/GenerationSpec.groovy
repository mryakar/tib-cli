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

  Pack pack = PacksSpec.pack('p', '1')
  Project project = Project.of(new Spec(1, 's', null, 'p', null))
  Generator generator = { Project p, Tree tree ->
    tree.own('Generated.java', 'class Generated {}\n')
    tree.once('Service.java', 'class Service {}\n')
  } as Generator

  def "stages the files of the generator and the state file"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    Generation.run(project, pack, generator, '1.2.3', tree)

    then:
    tree.changes()*.path() == ['.tib/state.yaml', 'Generated.java', 'Service.java']
    tree.changes()*.owner() == [Owner.GENERATOR, Owner.GENERATOR, Owner.DEVELOPER]

    and:
    State state = State.read(tree.read(State.PATH).get())
    state.tib() == new State.Tib('1.2.3', 1)
    state.pack() == new State.PackRef('p', '1')
    state.spec() == project.spec()
    state.files()*.path() == ['Generated.java', 'Service.java']
    state.files()*.owner() == [Owner.GENERATOR, Owner.DEVELOPER]
    state.files()[0].sha256() == Generation.sha256('class Generated {}\n')
  }

  def "a later run keeps the files of the earlier ones in the state"() {
    given:
    Tree first = Tree.at(dir)
    Generation.run(project, pack, generator, '1.2.3', first)
    first.apply()
    Files.writeString(dir.resolve('Service.java'), 'class Service { int edited; }\n')

    when:
    Tree second = Tree.at(dir)
    Generation.run(project, pack, { Project p, Tree tree ->
      tree.own('Other.java', 'class Other {}\n')
    } as Generator, '1.2.4', second)

    then: 'the developer file is not staged again, but stays on record as it was written'
    second.changes()*.path() == ['.tib/state.yaml', 'Other.java']
    State state = State.read(second.read(State.PATH).get())
    state.tib().version() == '1.2.4'
    state.files()*.path() == ['Generated.java', 'Other.java', 'Service.java']
    state.files()[2].sha256() == Generation.sha256('class Service {}\n')

    and:
    Generation.state(second).present
    Generation.state(Tree.at(Files.createDirectory(dir.resolve('empty')))).empty
  }

  def "the hash is the SHA-256 in hex"() {
    expect:
    Generation.sha256('') == 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
  }

  def "a digest that the runtime does not have"() {
    when:
    Generation.digest('NO-SUCH', 'x')

    then:
    thrown(IllegalStateException)
  }
}
