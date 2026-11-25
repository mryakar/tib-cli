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
package me.yakar.tib.pack.plain

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.generate.Project
import me.yakar.tib.pack.GradleWrapper
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
    new InitGenerator(new PlainPack()).generate(project(''), tree)
    List<Change> wrapper = tree.changes().findAll { it.path() in GradleWrapper.FILES }

    then: 'the files are Gradle\'s, as they are, without a header of tib\'s'
    wrapper*.path() == GradleWrapper.FILES.toSorted()
    wrapper.every { it.owner() == Owner.DEVELOPER && it.before() == null }
    wrapper.findAll { it.executable() }*.path() == ['gradlew']
    wrapper.each { assert it.after() == Files.readAllBytes(BUILD.resolve(it.path())) : it.path() }
  }

  def "a new service is wired by hand, and the routes of its API are tib's, without an entity yet"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(project('entities:\n  - name: A\n'), tree)
    Map<String, Change> files = tree.changes().collectEntries { [it.path(), it] }
    String routes = new String(files['src/main/java/com/example/s/Routes.java'].after(), 'UTF-8')
    String application = new String(files['src/main/java/com/example/s/SApplication.java'].after(), 'UTF-8')

    then:
    files['src/main/java/com/example/s/Routes.java'].owner() == Owner.GENERATOR
    routes.startsWith('// Written by tib on every run. Do not edit: your changes would be lost.\n')
    routes.contains('public static void add(HttpServer server, Transactions transactions) {}\n')
    !routes.contains('Handler')
    application.contains('    Routes.add(server, new Transactions(dataSource));\n')
    ['Config', 'Json', 'Transactions', 'SApplication'].every {
      files["src/main/java/com/example/s/${it}.java"].owner() == Owner.DEVELOPER
    }
  }

  def "the build compiles for the Java of the spec, starts the application, and the notes for agents name the Java: #yaml"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(project(yaml), tree)
    Map<String, String> files = texts(tree)

    then:
    files['build.gradle.kts'].contains("java { toolchain { languageVersion = JavaLanguageVersion.of($java) } }\n")
    files['build.gradle.kts'].contains('application { mainClass = "com.example.s.SApplication" }\n')
    files['build.gradle.kts'].contains('  mainClass = "com.example.s.TestSApplication"\n')
    files['AGENTS.md'].contains("A service in plain Java $java, scaffolded by tib")

    where:
    yaml         | java
    ''           | 21
    'java: 25\n' | 25
  }

  def "the configuration comes from the environment, with the defaults of a machine of development, which the notes name"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(new Project(SpecReader.parse('tib: 1\nservice: vehicle-service\n')), tree)
    Map<String, String> files = texts(tree)
    String config = files['src/main/java/com/example/vehicleservice/Config.java']

    then:
    config.contains('"DATABASE_URL", "jdbc:postgresql://localhost:5432/vehicle_service")')
    config.contains('environment.getOrDefault("DATABASE_USER", "vehicle_service")')
    config.contains('environment.getOrDefault("DATABASE_PASSWORD", "vehicle_service")')
    config.contains('Integer.parseInt(environment.getOrDefault("PORT", "8080"))')
    ['DATABASE_URL', 'DATABASE_USER', 'DATABASE_PASSWORD', 'PORT'].every {
      files['AGENTS.md'].contains("`$it`")
    }
  }

  def "a service with the test of its architecture gets ArchitectureTests and ArchUnit, and the notes tell of it: #tests"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(project("tests: $tests\n"), tree)
    Map<String, Change> files = tree.changes().collectEntries { [it.path(), it] }
    String build = new String(files['build.gradle.kts'].after(), 'UTF-8')
    String agents = new String(files['AGENTS.md'].after(), 'UTF-8')
    Change architecture = files['src/test/java/com/example/s/ArchitectureTests.java']

    then:
    (architecture != null) == archunit
    !archunit || architecture.owner() == Owner.DEVELOPER
    !archunit || new String(architecture.after(), 'UTF-8').contains('void handlersUseServicesAndNotRepositories()')
    !archunit || new String(architecture.after(), 'UTF-8').contains('  void onlyHandlersAndTheWiringUseHandlers() {\n    noClasses()\n        .that(not(HANDLERS).and(not(WIRING)))\n')
    build.contains("  testImplementation(\"com.tngtech.archunit:archunit:${new PlainPack().archunitVersion()}\")\n  testRuntimeOnly(") == archunit
    agents.contains('and `Page`.\n`ArchitectureTests`, in the tests of the base package,') == archunit
    agents.contains('their tests: ') == entities

    where:
    tests              | archunit | entities
    '[unit, archunit]' | true     | true
    '[archunit]'       | true     | false
    '[unit]'           | false    | true
  }

  def "the notes say what the files of tib's ask of the developer's, and where a query of the developer's goes: #architecture"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(project("architecture: $architecture\n"), tree)
    String agents = texts(tree)['AGENTS.md']

    then:
    agents.contains('.\n`X` keeps a constructor without parameters: the files of tib\'s make an `X` and set its values.\nA query of your own goes in ')
    agents.contains(query + '\n`ErrorHandler`, in the base package, answers a request for an entity that is not there with 404\n')

    where:
    architecture | query
    'anemic'     | 'A query of your own goes in a class of yours, which takes the connection of the unit of work from\n`Transactions.connection()`.'
    'ddd'        | 'A query of your own goes in a class of yours, which takes the connection of the unit of work from\n`Transactions.connection()`.'
    'hexagonal'  | 'A query of your own goes in `XPersistenceAdapter`, which takes the connection of the unit of work\nfrom `Transactions.connection()`.'
  }

  def "a service with a Dockerfile gets one for the Java of its spec, which runs what Gradle installs, and the notes tell how to build the image: #yaml"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(project(yaml), tree)
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
    !docker || dockerfile.contains('RUN sh gradlew installDist --no-daemon\n')
    !docker || dockerfile.contains('ENTRYPOINT ["/application/bin/s"]\n')
    !docker || new String(files['.dockerignore'].after(), 'UTF-8').contains('\nbuild/\n')
    agents.contains('- `docker build -t s .` builds an image of the service from its `Dockerfile`, which\n  is yours.') == docker

    where:
    yaml                       | docker | java
    ''                         | false  | 21
    'docker: true\n'           | true   | 21
    'docker: true\njava: 25\n' | true   | 25
  }

  def "the tests that start the service come with the integration and the API tests, and the notes tell of the tests: #tests"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new InitGenerator(new PlainPack()).generate(project("tests: $tests\n"), tree)
    Map<String, String> files = texts(tree)
    String agents = files['AGENTS.md']

    then:
    files.containsKey('src/test/java/com/example/s/SApplicationTests.java') == application
    files.containsKey('src/test/java/com/example/s/TestService.java') == service
    files.containsKey('src/test/java/com/example/s/TestSApplication.java')
    files['src/test/java/com/example/s/TestDatabase.java'].contains('  public static void clear(String... tables) {\n')
    agents.contains('- `./gradlew build` compiles, tests and checks. The tests start PostgreSQL in a container, so\n  Docker has to run.\n') == container
    agents.contains('- `./gradlew build` compiles, tests and checks.\n') == !container
    agents.contains(notes)

    where:
    tests                 | application | service | container | notes
    '[integration, unit]' | true        | true    | true      | 'are yours, and so are\ntheir tests: `XRepositoryTests` against PostgreSQL in a container, and\n`XServiceTests` with the repository and the mapper mocked. Their sample entities and DTOs come\n'
    '[api]'               | false       | true    | true      | 'are yours, and so are\ntheir tests: `XApiTests` against PostgreSQL in a container. Their sample'
    '[unit]'              | false       | false   | false     | 'their tests: `XServiceTests` with the repository and the mapper mocked. Their sample'
    '[]'                  | false       | false   | false     | 'and the migration are yours.\n`X` keeps a constructor without parameters: the files of tib\'s make an `X` and set its values.\n'
  }

  private static Project project(String yaml) {
    new Project(SpecReader.parse("tib: 1\nservice: s\npack: plain\n$yaml"))
  }

  private static Map<String, String> texts(Tree tree) {
    tree.changes().collectEntries {
      [it.path(), new String(it.after(), 'UTF-8')]
    }
  }
}
