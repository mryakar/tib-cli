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
import java.util.concurrent.TimeUnit
import me.yakar.tib.ddl.DdlReader
import me.yakar.tib.generate.Pack
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.spec.TestKind
import me.yakar.tib.state.State
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

/**
 * The services of the plain pack of the example spec, of its DDL and of the corner spec of the
 * snapshots, generated and then built with their own Gradle wrapper, as a user would build them.
 * Where Docker runs Linux containers, the build includes the tests of the service, which start
 * PostgreSQL in a container, and the image of a service with a Dockerfile is built; elsewhere the
 * build compiles the service and its tests. On Linux under CI, Docker has to be there.
 */
class GeneratedServiceSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  /** The JDKs of the build of tib, among which the build of a service finds the one of its Java. */
  static final String JDKS = System.getProperty('tib.jdks')
  static final boolean LINUX = System.getProperty('os.name') == 'Linux'
  static final boolean WINDOWS = System.getProperty('os.name').startsWith('Windows')

  /** The spec of the example service of the documentation, of the plain pack. */
  static final String EXAMPLE = Files.readString(EXAMPLES.resolve('vehicle-service.yaml')).replace('pack: default\n', 'pack: plain\n')

  @TempDir
  Path dir

  @TempDir
  Path logs

  def "the service generated from the #input builds"() {
    given: 'the service, with its entities'
    Project project = new Project(spec)
    Tree tree = Tree.at(dir)
    Pack pack = new PlainPack()
    pack.init().generate(project, tree)
    pack.entity().generate(project, tree)
    tree.apply(State.PATH)
    boolean docker = docker()
    assert docker || !LINUX || !System.getenv('CI') : 'the build of the service needs Docker under CI'
    Path log = logs.resolve('gradle.log')
    Process gradle = null

    when:
    String gradlew = dir.resolve(WINDOWS ? 'gradlew.bat' : 'gradlew').toString()
    List<String> command = [
      gradlew,
      '-p',
      dir.toString(),
      'build',
      '--console=plain',
      '--no-daemon',
      "-Porg.gradle.java.installations.paths=$JDKS".toString()
    ]
    if (!docker) {
      // Without Docker, only the tests that need no database run: the unit tests and the test of the
      // architecture. With the task test left out, nothing in the build compiles the tests.
      if (spec.tests().contains(TestKind.UNIT) || spec.tests().contains(TestKind.ARCHUNIT)) {
        command.addAll(['test', '--tests', '*ServiceTests', '--tests', '*.ArchitectureTests'])
      } else {
        command.addAll(['testClasses', '-x', 'test'])
      }
    }
    gradle = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start()
    assert gradle.waitFor(15, TimeUnit.MINUTES) : 'the build of the service took too long'
    println Files.readString(log)

    then:
    gradle.exitValue() == 0
    Files.readString(dir.resolve('build.gradle.kts')).contains("JavaLanguageVersion.of(${spec.java()})")
    String classes = project.packagePath() + '/' + project.applicationClass()
    Files.exists(dir.resolve("build/classes/java/main/${classes}.class"))
    Files.exists(dir.resolve("build/classes/java/main/${project.packagePath()}/Routes.class"))
    Files.exists(dir.resolve("build/classes/java/test/${project.packagePath()}/Test${project.applicationClass()}.class"))

    and: 'the tests are of the kinds of the spec; every one compiles, and with Docker, or without a database, passes'
    List<String> tests = tests(dir)
    tests.contains("${classes}Tests".toString()) == spec.tests().contains(TestKind.INTEGRATION)
    tests.any { it.endsWith('ServiceTests') } == spec.tests().contains(TestKind.UNIT)
    tests.any { it.endsWith('ApiTests') } == spec.tests().contains(TestKind.API)
    tests.any {
      it.endsWith('RepositoryTests') || it.endsWith('PersistenceAdapterTests')
    } == spec.tests().contains(TestKind.INTEGRATION)
    tests.contains("${project.packagePath()}/ArchitectureTests".toString()) == spec.tests().contains(TestKind.ARCHUNIT)
    tests.each { test ->
      assert Files.exists(dir.resolve("build/classes/java/test/${test}.class")) : test
      if (docker || test.endsWith('ServiceTests') || test.endsWith('/ArchitectureTests')) {
        String result = Files.readString(dir.resolve("build/test-results/test/TEST-${test.replace('/', '.')}.xml"))
        assert result.contains('failures="0"') && result.contains('errors="0"') && !result.contains('tests="0"') : test
      }
    }

    and: 'with Docker, the Dockerfile of a service that has one builds its image'
    !(docker && spec.docker()) || image(dir, project.name(), logs.resolve('docker.log'))

    cleanup:
    gradle?.destroyForcibly()

    where:
    input             | spec
    'example spec'    | SpecReader.parse(EXAMPLE)
    // Only the unit tests and the test of the architecture, which need no database.
    'example spec, without a database' | SpecReader.parse(EXAMPLE.replace('pack: plain\n', 'pack: plain\ntests: [unit, archunit]\n'))
    // The tables of the DDL, with the tests of the API only.
    'example DDL'     | DdlReader.parse(Files.readString(EXAMPLES.resolve('vehicle-service.sql')), SpecReader.parse('tib: 1\nservice: vehicle-service\npack: plain\ntests: [api]\n'))
    // The corner spec, on Java 25, with a Dockerfile and every kind of test, is a resource of the unit
    // tests, next to their snapshots.
    'corner spec'     | SpecReader.parse(GeneratedServiceSpec.getResource('corner-service.yaml').text)
    // The corner spec in the DDD architecture.
    'corner spec, DDD' | SpecReader.parse(GeneratedServiceSpec.getResource('corner-service.yaml').text.replace('java: 25\n', 'java: 25\narchitecture: ddd\n'))
    // The corner spec in the hexagonal architecture.
    'corner spec, hexagonal' | SpecReader.parse(GeneratedServiceSpec.getResource('corner-service.yaml').text.replace('java: 25\n', 'java: 25\narchitecture: hexagonal\n'))
  }

  /** Builds the image of the service from its Dockerfile, prints the log, and removes the image. */
  private static boolean image(Path service, String name, Path log) {
    String tag = "tib-acceptance-plain-$name"
    Process build = new ProcessBuilder('docker', 'build', '--tag', tag, service.toString())
        .redirectErrorStream(true).redirectOutput(log.toFile()).start()
    try {
      assert build.waitFor(20, TimeUnit.MINUTES) : 'the build of the image took too long'
      println Files.readString(log)
      return build.exitValue() == 0
    } finally {
      build.destroyForcibly()
      new ProcessBuilder('docker', 'image', 'rm', '--force', tag).redirectErrorStream(true).start().waitFor(1, TimeUnit.MINUTES)
    }
  }

  /** The test classes of the service, as paths without their extension: the files that end in Tests. */
  private static List<String> tests(Path service) {
    Path sources = service.resolve('src/test/java')
    Files.walk(sources).withCloseable { paths ->
      paths.filter { it.fileName.toString().endsWith('Tests.java') }.collect {
        sources.relativize(it).toString().replace('\\', '/') - '.java'
      }.sort()
    }
  }

  /**
   * Whether Docker runs Linux containers here: the tests of the service need one for PostgreSQL,
   * and its Dockerfile builds a Linux image. Docker on a Windows runner may run Windows containers
   * only.
   */
  private static boolean docker() {
    try {
      Process docker = new ProcessBuilder('docker', 'info', '--format', '{{.OSType}}').redirectErrorStream(true).start()
      if (!docker.waitFor(30, TimeUnit.SECONDS)) {
        docker.destroyForcibly()
        return false
      }
      return docker.exitValue() == 0 && docker.inputStream.text.trim() == 'linux'
    } catch (IOException ignored) {
      return false
    }
  }
}
