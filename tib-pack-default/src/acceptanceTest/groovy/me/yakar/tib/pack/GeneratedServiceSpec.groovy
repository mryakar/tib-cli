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
import java.util.concurrent.TimeUnit
import me.yakar.tib.ddl.DdlReader
import me.yakar.tib.generate.Pack
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

/**
 * The services of the example spec, of its DDL and of the corner spec of the snapshots, generated
 * and then built with Gradle, as a user would build them. Where Docker runs Linux containers, the
 * build includes the test of the service, which starts PostgreSQL in a container; elsewhere the
 * build compiles the service and its test. On Linux under CI, Docker has to be there.
 */
class GeneratedServiceSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))
  static final String GRADLEW = System.getProperty('tib.gradlew')
  static final boolean LINUX = System.getProperty('os.name') == 'Linux'

  @TempDir
  Path dir

  @TempDir
  Path logs

  def "the service generated from the #input builds"() {
    given: 'the service, with its entities'
    Project project = new Project(spec)
    Tree tree = Tree.at(dir)
    Pack pack = new DefaultPack()
    pack.init().generate(project, tree)
    pack.entity().generate(project, tree)
    tree.apply()
    boolean docker = docker()
    assert docker || !LINUX || !System.getenv('CI') : 'the build of the service needs Docker under CI'
    Path log = logs.resolve('gradle.log')
    Process gradle = null

    when:
    List<String> command = [GRADLEW, '-p', dir.toString(), 'build', '--console=plain', '--no-daemon']
    if (!docker) {
      // Without its test, nothing in the build compiles the test of the service: it is asked for.
      command.addAll(['testClasses', '-x', 'test'])
    }
    gradle = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start()
    assert gradle.waitFor(15, TimeUnit.MINUTES) : 'the build of the service took too long'
    println Files.readString(log)

    then:
    assert gradle.exitValue() == 0 : Files.readString(log)
    String classes = project.packagePath() + '/' + project.applicationClass()
    Files.exists(dir.resolve("build/classes/java/main/${classes}.class"))
    Files.exists(dir.resolve("build/classes/java/test/${project.packagePath()}/Test${project.applicationClass()}.class"))

    and: 'every test of the service compiles, and with Docker, passes'
    List<String> tests = tests(dir)
    tests.contains("${classes}Tests".toString())
    tests.size() > 1
    tests.each { test ->
      assert Files.exists(dir.resolve("build/classes/java/test/${test}.class")) : test
      if (docker) {
        String result = Files.readString(dir.resolve("build/test-results/test/TEST-${test.replace('/', '.')}.xml"))
        assert result.contains('failures="0"') && result.contains('errors="0"') && !result.contains('tests="0"') : test
      }
    }

    cleanup:
    gradle?.destroyForcibly()

    where:
    input             | spec
    'example spec'    | SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml'))
    'example DDL'     | DdlReader.read(EXAMPLES.resolve('vehicle-service.sql'), SpecReader.parse('tib: 1\nservice: vehicle-service\n'))
    // The corner spec is a resource of the unit tests, next to their snapshots.
    'corner spec'     | SpecReader.parse(GeneratedServiceSpec.getResource('corner-service.yaml').text)
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
   * Whether Docker runs Linux containers here: the test of the service needs one for its database.
   * Docker on a Windows runner may run Windows containers only.
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
