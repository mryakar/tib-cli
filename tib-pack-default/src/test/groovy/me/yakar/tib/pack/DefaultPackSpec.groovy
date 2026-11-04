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
import me.yakar.tib.ddl.DdlReader
import me.yakar.tib.generate.Generation
import me.yakar.tib.generate.Pack
import me.yakar.tib.generate.Packs
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.tree.Change
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class DefaultPackSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))
  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))
  static final Path ACTUAL = Path.of(System.getProperty('tib.snapshots.actual'))

  /** The example service of the documentation. */
  static final Spec VEHICLES = SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml'))

  /** The corners of the entity layer that the example service does not reach. */
  static final Spec CORNERS = SpecReader.parse(DefaultPackSpec.getResource('corner-service.yaml').text)

  /** The first line of a generated file, without its comment marks, starts with one of these. */
  static final Map<Owner, List<String>> HEADERS = [
    (Owner.GENERATOR): ['Written by tib on every run. Do not edit: your changes would be lost.',],
    (Owner.DEVELOPER): [
      'Written by tib when the service was created; it is yours from then on.',
      'Written by tib when the entity was created; it is yours from then on.',
      'Written by tib when the entity was created. A change of the schema is a new migration.',
    ],
  ]

  @TempDir
  Path dir

  @TempDir
  Path other

  def "the pack says what it is, and the engine finds it"() {
    given:
    DefaultPack pack = new DefaultPack()

    expect:
    pack.name() == 'default'
    pack.version() == '0.1.0'
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
    when:
    List<Change> changes = generate(VEHICLES, dir).changes()

    then:
    changes.findAll { it.owner() == Owner.GENERATOR }*.path()*.replaceAll('.*/', '') == [
      'DepotDto.java',
      'DepotGenerated.java',
      'DepotMapper.java',
      'VehicleDto.java',
      'VehicleGenerated.java',
      'VehicleMapper.java',
    ]
    snapshot('vehicle-service', changes)
  }

  def "generates the corners of the entity layer as the snapshot says"() {
    given: 'a migration by hand, and a repeatable one'
    Tree tree = Tree.at(dir)
    Files.createDirectories(dir.resolve(EntityGenerator.MIGRATIONS))
    Files.writeString(dir.resolve(EntityGenerator.MIGRATIONS).resolve('V3__by_hand.sql'), '-- mine\n')
    Files.writeString(dir.resolve(EntityGenerator.MIGRATIONS).resolve('R__views.sql'), '-- repeatable\n')

    when:
    new DefaultPack().entity().generate(new Project(CORNERS), tree)

    then:
    snapshot('corner-service', tree.changes())
  }

  def "the first line of every generated file states its owner: #name"() {
    when:
    List<Change> changes = generate(spec, dir).changes()

    then:
    changes.each { change ->
      String header = change.after().readLines()[0].replaceFirst(/^(\/\/|#|--|<!--) /, '')
      assert HEADERS[change.owner()].any { header.startsWith(it) } : change.path()
    }

    where:
    name              | spec
    'vehicle-service' | VEHICLES
    'corner-service'  | CORNERS
  }

  def "the same spec gives the same files, the state included: #name"() {
    when:
    Tree first = Tree.at(dir)
    Tree second = Tree.at(other)
    [first, second].each { tree ->
      Generation.run(new Project(spec), [], new DefaultPack(), { it.init() }, '0.0.0', tree)
      Generation.run(new Project(spec), [], new DefaultPack(), { it.entity() }, '0.0.0', tree)
    }

    then:
    first.changes()*.path().contains('.tib/state.yaml')
    first.changes()*.path() == second.changes()*.path()
    first.changes()*.after() == second.changes()*.after()

    where:
    name              | spec
    'vehicle-service' | VEHICLES
    'corner-service'  | CORNERS
  }

  def "a second run on the generated service changes nothing"() {
    given:
    generate(VEHICLES, dir).apply()

    expect:
    generate(VEHICLES, dir).changes().empty
  }

  def "the migrations of the example service read back into its spec"() {
    given:
    Tree tree = Tree.at(dir)
    new DefaultPack().entity().generate(new Project(VEHICLES), tree)

    when:
    String migrations = tree.list(EntityGenerator.MIGRATIONS).collect {
      tree.read(it).orElseThrow()
    }.join('\n')

    then:
    DdlReader.parse(migrations, VEHICLES) == VEHICLES
  }

  /** The service of the spec with its entities, staged in a tree at the directory. */
  private static Tree generate(Spec spec, Path dir) {
    Project project = new Project(spec)
    Tree tree = Tree.at(dir)
    Pack pack = new DefaultPack()
    pack.init().generate(project, tree)
    pack.entity().generate(project, tree)
    return tree
  }

  /**
   * The files are the ones of the snapshot, each as the snapshot says. What was generated is
   * written where the build asks for it, to be copied over the snapshot on purpose.
   */
  private static void snapshot(String name, List<Change> changes) {
    Path actual = ACTUAL.resolve(name)
    actual.toFile().deleteDir()
    changes.each { change ->
      Path file = actual.resolve(change.path())
      Files.createDirectories(file.parent)
      Files.writeString(file, change.after())
    }
    Path expected = SNAPSHOTS.resolve(name)
    List<String> files = Files.walk(expected).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) }.collect {
        expected.relativize(it).toString().replace('\\', '/')
      }.sort()
    }
    assert files == changes*.path()
    changes.each { change ->
      assert change.after() == Files.readString(expected.resolve(change.path())) : change.path()
    }
  }
}
