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
import me.yakar.tib.generate.Pack
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

/**
 * The example service of the documentation, generated and then built with Gradle, as a user
 * would build it. Where Docker runs Linux containers, the build includes the test of the service,
 * which starts PostgreSQL in a container; elsewhere the build compiles the service and its test.
 */
class GeneratedServiceSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))
  static final String GRADLEW = System.getProperty('tib.gradlew')

  @TempDir
  Path dir

  def "the generated example service builds"() {
    given: 'the service, with its entities'
    Project project = Project.of(SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml')))
    Tree tree = Tree.at(dir)
    Pack pack = new DefaultPack()
    pack.init().generate(project, tree)
    pack.entity().generate(project, tree)
    tree.apply()
    boolean docker = docker()

    when:
    List<String> command = [GRADLEW, '-p', dir.toString(), 'build', '--console=plain', '--no-daemon']
    if (!docker) {
      // Without its test, nothing in the build compiles the test of the service: it is asked for.
      command.addAll(['testClasses', '-x', 'test'])
    }
    Process gradle = new ProcessBuilder(command).redirectErrorStream(true).start()
    StringBuilder output = new StringBuilder()
    gradle.inputStream.eachLine { line ->
      println line
      output.append(line).append('\n')
    }
    gradle.waitFor(15, TimeUnit.MINUTES)

    then:
    assert gradle.exitValue() == 0 : output
    Files.exists(dir.resolve('build/classes/java/main/com/example/vehicleservice/vehicle/VehicleController.class'))
    Files.exists(dir.resolve('build/classes/java/test/com/example/vehicleservice/VehicleServiceApplicationTests.class'))
    !docker || Files.exists(dir.resolve('build/test-results/test/TEST-com.example.vehicleservice.VehicleServiceApplicationTests.xml'))
    !docker || Files.readString(dir.resolve('build/test-results/test/TEST-com.example.vehicleservice.VehicleServiceApplicationTests.xml')).contains('failures="0"')
  }

  /**
   * Whether Docker runs Linux containers here: the test of the service needs one for its database.
   * Docker on a Windows runner may run Windows containers only.
   */
  private static boolean docker() {
    try {
      Process docker = new ProcessBuilder('docker', 'info', '--format', '{{.OSType}}').redirectErrorStream(true).start()
      String os = docker.inputStream.text.trim()
      return docker.waitFor() == 0 && os == 'linux'
    } catch (IOException ignored) {
      return false
    }
  }
}
