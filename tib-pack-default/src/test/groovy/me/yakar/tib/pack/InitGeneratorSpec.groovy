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

class InitGeneratorSpec extends Specification {

  /** The root of this build, whose Gradle wrapper the pack carries. */
  static final Path BUILD = Path.of(System.getProperty('tib.root'))

  @TempDir
  Path dir

  def "a new service comes with the Gradle wrapper of this build, written once, and gradlew is run"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new DefaultPack()).generate(new Project(SpecReader.parse('tib: 1\nservice: s\n')), tree)
    List<Change> wrapper = tree.changes().findAll { it.path() in InitGenerator.WRAPPER }

    then: 'the files are Gradle\'s, as they are, without a header of tib\'s'
    wrapper*.path() == InitGenerator.WRAPPER.toSorted()
    wrapper.every { it.owner() == Owner.DEVELOPER && it.before() == null }
    wrapper.findAll { it.executable() }*.path() == ['gradlew']
    wrapper.each { assert it.after() == Files.readAllBytes(BUILD.resolve(it.path())) : it.path() }
  }
}
