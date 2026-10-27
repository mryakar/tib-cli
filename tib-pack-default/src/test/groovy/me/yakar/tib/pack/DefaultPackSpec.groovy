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
import me.yakar.tib.generate.Pack
import me.yakar.tib.generate.Packs
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Change
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class DefaultPackSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))
  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))

  @TempDir
  Path dir

  def "the pack says what it is, and the engine finds it"() {
    given:
    DefaultPack pack = new DefaultPack()

    expect:
    pack.name() == 'default'
    pack.version() == '0.1.0'
    pack.bootVersion() == '3.5.16'
    pack.javaVersion() == '21'
    pack.postgresVersion() == '17'
    Packs.find('default') instanceof DefaultPack
    Packs.find('default@0.1.0') instanceof DefaultPack
  }

  def "the properties of the pack have to be readable"() {
    when:
    DefaultPack.load(new InputStream() {
          int read() {
            throw new IOException('unreadable')
          }
        })

    then:
    thrown(UncheckedIOException)
  }

  def "generates the example service of the documentation as the snapshot says"() {
    given:
    Project project = Project.of(SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml')))
    Tree tree = Tree.at(dir)

    when:
    new DefaultPack().init().generate(project, tree)
    List<Change> changes = tree.changes()
    changes.each { change ->
      Path actual = Path.of('build', 'snapshots', 'vehicle-service', change.path())
      Files.createDirectories(actual.parent)
      Files.writeString(actual, change.after())
    }

    then:
    changes*.path() == [
      '.gitignore',
      'AGENTS.md',
      'build.gradle.kts',
      'settings.gradle.kts',
      'src/main/java/com/example/vehicleservice/VehicleServiceApplication.java',
      'src/main/resources/application.yaml',
      'src/test/java/com/example/vehicleservice/TestcontainersConfiguration.java',
      'src/test/java/com/example/vehicleservice/VehicleServiceApplicationTests.java',
    ]
    changes*.owner() == [Owner.DEVELOPER, Owner.GENERATOR] + [Owner.DEVELOPER] * 6

    and: 'every file is as the snapshot says; if not, the new one is under build/snapshots to be copied on purpose'
    changes.each { change ->
      Path expected = SNAPSHOTS.resolve('vehicle-service').resolve(change.path())
      assert Files.exists(expected) : "no snapshot for ${change.path()}"
      assert change.after() == Files.readString(expected)
    }
  }

  def "a second run on the generated service changes nothing"() {
    given:
    Project project = Project.of(SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml')))
    Tree tree = Tree.at(dir)
    Pack pack = new DefaultPack()
    pack.init().generate(project, tree)
    tree.apply()

    when:
    Tree again = Tree.at(dir)
    pack.init().generate(project, again)

    then:
    again.changes().empty
  }
}
