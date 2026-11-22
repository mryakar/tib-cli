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

  def "the build compiles for the Java of the spec, and the notes for agents name it: #yaml"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new DefaultPack()).generate(new Project(SpecReader.parse("tib: 1\nservice: s\n$yaml")), tree)
    Map<String, String> files = tree.changes().collectEntries {
      [it.path(), new String(it.after(), 'UTF-8')]
    }

    then:
    files['build.gradle.kts'].contains("java { toolchain { languageVersion = JavaLanguageVersion.of($java) } }\n")
    files['AGENTS.md'].contains(" service on Java $java, ")

    where:
    yaml         | java
    ''           | 21
    'java: 25\n' | 25
  }

  def "a service with a Dockerfile gets one for the Java of its spec, and the notes tell how to build the image: #yaml"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new DefaultPack()).generate(new Project(SpecReader.parse("tib: 1\nservice: s\n$yaml")), tree)
    Map<String, Change> files = tree.changes().collectEntries { [it.path(), it] }
    String dockerfile = files['Dockerfile'] ? new String(files['Dockerfile'].after(), 'UTF-8') : null
    String agents = new String(files['AGENTS.md'].after(), 'UTF-8')

    then:
    files.containsKey('Dockerfile') == docker
    files.containsKey('.dockerignore') == docker
    !docker || [files['Dockerfile'], files['.dockerignore']].every { it.owner() == Owner.DEVELOPER }
    !docker || dockerfile.startsWith('# Written by tib when the service was created; it is yours from then on.\n')
    !docker || dockerfile.contains("FROM eclipse-temurin:$java-jdk AS build\n")
    !docker || dockerfile.contains("FROM eclipse-temurin:$java-jre\n")
    !docker || new String(files['.dockerignore'].after(), 'UTF-8').contains('\nbuild/\n')
    agents.contains('- `docker build -t s .` builds an image of the service from its `Dockerfile`, which\n  is yours.') == docker

    where:
    yaml                       | docker | java
    ''                         | false  | 21
    'docker: true\n'           | true   | 21
    'docker: true\njava: 25\n' | true   | 25
  }

  def "the test of the application comes with the integration tests, and the notes tell of the tests: #tests"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new DefaultPack()).generate(new Project(SpecReader.parse("tib: 1\nservice: s\ntests: $tests\n")), tree)
    Map<String, String> files = tree.changes().collectEntries {
      [it.path(), new String(it.after(), 'UTF-8')]
    }
    String agents = files['AGENTS.md']

    then:
    files.containsKey('src/test/java/com/example/s/SApplicationTests.java') == application
    files.containsKey('src/test/java/com/example/s/TestSApplication.java')
    agents.contains('- `./gradlew build` compiles, tests and checks. The tests start PostgreSQL in a container, so\n  Docker has to run.\n') == container
    agents.contains('- `./gradlew build` compiles, tests and checks.\n') == !container
    agents.contains(notes)

    where:
    tests                 | application | container | notes
    '[integration, unit]' | true        | true      | 'are yours, and so are\ntheir tests: `XRepositoryTests` against PostgreSQL in a container, and\n`XServiceTests` with the repository and the mapper mocked. Their sample entities and DTOs come\n'
    '[api]'               | false       | true      | 'are yours, and so are\ntheir tests: `XApiTests` against PostgreSQL in a container. Their sample'
    '[unit]'              | false       | false     | 'their tests: `XServiceTests` with the repository and the mapper mocked. Their sample'
    '[]'                  | false       | false     | 'and the migration are yours. `ConflictHandler`, in the base package, answers a\n'
  }
}
