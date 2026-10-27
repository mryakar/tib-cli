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
    Pack pack = new DefaultPack()

    when:
    pack.init().generate(project, tree)
    pack.entity().generate(project, tree)
    List<Change> changes = tree.changes()

    then:
    changes*.path() == [
      '.gitignore',
      'AGENTS.md',
      'build.gradle.kts',
      'settings.gradle.kts',
      'src/main/java/com/example/vehicleservice/VehicleServiceApplication.java',
      'src/main/java/com/example/vehicleservice/depot/Depot.java',
      'src/main/java/com/example/vehicleservice/depot/DepotController.java',
      'src/main/java/com/example/vehicleservice/depot/DepotDto.java',
      'src/main/java/com/example/vehicleservice/depot/DepotGenerated.java',
      'src/main/java/com/example/vehicleservice/depot/DepotMapper.java',
      'src/main/java/com/example/vehicleservice/depot/DepotRepository.java',
      'src/main/java/com/example/vehicleservice/depot/DepotService.java',
      'src/main/java/com/example/vehicleservice/vehicle/Vehicle.java',
      'src/main/java/com/example/vehicleservice/vehicle/VehicleController.java',
      'src/main/java/com/example/vehicleservice/vehicle/VehicleDto.java',
      'src/main/java/com/example/vehicleservice/vehicle/VehicleGenerated.java',
      'src/main/java/com/example/vehicleservice/vehicle/VehicleMapper.java',
      'src/main/java/com/example/vehicleservice/vehicle/VehicleRepository.java',
      'src/main/java/com/example/vehicleservice/vehicle/VehicleService.java',
      'src/main/resources/application.yaml',
      'src/main/resources/db/migration/V1__create_depots.sql',
      'src/main/resources/db/migration/V2__create_vehicle.sql',
      'src/test/java/com/example/vehicleservice/TestcontainersConfiguration.java',
      'src/test/java/com/example/vehicleservice/VehicleServiceApplicationTests.java',
    ]
    changes.findAll { it.owner() == Owner.GENERATOR }*.path()*.replaceAll('.*/', '') == [
      'AGENTS.md',
      'DepotDto.java',
      'DepotGenerated.java',
      'DepotMapper.java',
      'DepotRepository.java',
      'VehicleDto.java',
      'VehicleGenerated.java',
      'VehicleMapper.java',
      'VehicleRepository.java',
    ]
    snapshot('vehicle-service', changes)
  }

  def "generates the corners of the entity layer as the snapshot says"() {
    given:
    Project project = Project.of(SpecReader.read(SNAPSHOTS.resolve('corner-service/spec.yaml')))
    Tree tree = Tree.at(dir)
    Files.createDirectories(dir.resolve('src/main/resources/db/migration'))
    Files.writeString(dir.resolve('src/main/resources/db/migration/V3__by_hand.sql'), '-- mine\n')
    Files.writeString(dir.resolve('src/main/resources/db/migration/R__views.sql'), '-- repeatable\n')

    when:
    new DefaultPack().entity().generate(project, tree)
    List<Change> changes = tree.changes()

    then: 'Branch comes first, Tag refers to it; the migrations continue after the one by hand'
    changes*.path().findAll { it.endsWith('.sql') } == [
      'src/main/resources/db/migration/V4__create_branch.sql',
      'src/main/resources/db/migration/V5__create_note.sql',
      'src/main/resources/db/migration/V6__create_tag.sql',
      'src/main/resources/db/migration/V7__create_plain.sql',
    ]
    changes*.path().findAll {
      it.contains('/note/')
    }*.replaceAll('.*/', '') == ['Note.java', 'NoteGenerated.java', 'NoteRepository.java']
    changes*.path().findAll {
      it.contains('/plain/')
    }*.replaceAll('.*/', '') == ['Plain.java', 'PlainDto.java', 'PlainGenerated.java', 'PlainRepository.java']
    snapshot('corner-service', changes)
  }

  def "a second run on the generated service changes nothing"() {
    given:
    Project project = Project.of(SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml')))
    Tree tree = Tree.at(dir)
    Pack pack = new DefaultPack()
    pack.init().generate(project, tree)
    pack.entity().generate(project, tree)
    tree.apply()

    when:
    Tree again = Tree.at(dir)
    pack.init().generate(project, again)
    pack.entity().generate(project, again)

    then:
    again.changes().empty
  }

  def "entities come after the entities they refer to, cycles in the order of the spec"() {
    given:
    Project project = Project.of(SpecReader.read('''\
tib: 1
service: s
entities:
  - name: A
    relations:
      - { kind: manyToOne, target: B }
  - name: B
    relations:
      - { kind: manyToOne, target: A }
  - name: C
    relations:
      - { kind: manyToOne, target: B }
      - { kind: manyToOne, target: B, field: other }
'''))

    expect:
    EntityGenerator.ordered(project.spec().entities().collectEntries {
      [it.name(), it]
    })*.name() == ['B', 'A', 'C']
  }

  /** Every file is as the snapshot says; if not, the new one is under build/snapshots, to be copied on purpose. */
  private static void snapshot(String name, List<Change> changes) {
    changes.each { change ->
      Path actual = Path.of('build', 'snapshots', name, change.path())
      Files.createDirectories(actual.parent)
      Files.writeString(actual, change.after())
    }
    changes.each { change ->
      Path expected = SNAPSHOTS.resolve(name).resolve(change.path())
      assert Files.exists(expected) : "no snapshot for ${change.path()}"
      assert change.after() == Files.readString(expected)
    }
  }
}
