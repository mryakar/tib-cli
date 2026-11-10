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

import com.google.googlejavaformat.java.Formatter
import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.ddl.DdlReader
import me.yakar.tib.generate.Generation
import me.yakar.tib.generate.Pack
import me.yakar.tib.generate.Packs
import me.yakar.tib.generate.Project
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.state.State
import me.yakar.tib.tree.Change
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class DefaultPackSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))
  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))
  static final Path ACTUAL = Path.of(System.getProperty('tib.snapshots.actual'))

  /** The version of tib that the build makes. */
  static final String VERSION = System.getProperty('tib.version')

  /** The version of the tib that runs in a test. */
  static final String TIB = '0.0.0'

  /** The example service of the documentation. */
  static final Spec VEHICLES = SpecReader.parse(Files.readString(EXAMPLES.resolve('vehicle-service.yaml')))

  /** The corners of the entity layer that the example service does not reach. */
  static final String CORNER_SPEC = DefaultPackSpec.getResource('corner-service.yaml').text
  static final Spec CORNERS = SpecReader.parse(CORNER_SPEC)

  /**
   * The corner spec as its migrations read back. Two types do not come back as the spec gives them:
   * a BigDecimal with a precision and no scale is NUMERIC(10, 0), which reads back as a Long, and a
   * String without a length is VARCHAR(255), which reads back with that length.
   */
  static final Spec CORNERS_READ_BACK = SpecReader.parse(CORNER_SPEC
  .replace('{ name: amount, type: BigDecimal, precision: 10 }', '{ name: amount, type: Long }')
  .replace('type: String, nullable: false }', 'type: String, length: 255, nullable: false }')
  .replace('{ name: label, type: String }', '{ name: label, type: String, length: 255 }'))

  /** The first line of a generated file, without its comment marks, starts with one of these. */
  static final Map<Owner, List<String>> HEADERS = [
    (Owner.GENERATOR): ['Written by tib on every run. Do not edit: your changes would be lost.',],
    (Owner.DEVELOPER): [
      'Written by tib when the service was created; it is yours from then on.',
      'Written by tib when the entity was created; it is yours from then on.',
      'Written by tib with the first entity of the service; it is yours from then on.',
      'Written by tib when the entity was created. A change of the schema is a new migration.',
    ],
  ]

  @TempDir
  Path dir

  @TempDir
  Path other

  def "the pack says what it is, with the version of tib, and the engine finds it"() {
    given:
    DefaultPack pack = new DefaultPack()

    expect:
    pack.name() == 'default'
    pack.version() == VERSION
    pack.stack() == 'Spring Boot 4'
    Packs.find('default') instanceof DefaultPack
    Packs.find("default@$VERSION") instanceof DefaultPack
  }

  def "what the pack carries has to be readable: #what"() {
    when:
    read(new InputStream() {
          int read() {
            throw new IOException('unreadable')
          }
        })

    then:
    thrown(UncheckedIOException)

    where:
    what             | read
    'its properties' | { InputStream stream -> DefaultPack.load(stream) }
    'a file'         | { InputStream stream -> DefaultPack.bytes(stream) }
  }

  def "generates the example service of the documentation as the snapshot says"() {
    when:
    List<Change> changes = generate(VEHICLES, dir).changes()

    then:
    snapshot('vehicle-service', changes)
  }

  def "generates the corners of the entity layer as the snapshot says"() {
    when:
    List<Change> changes = corners(dir).changes()

    then:
    snapshot('corner-service', changes)
  }

  def "a machine in Turkish, where the upper case of i is not I, generates the same services"() {
    given:
    Locale locale = Locale.default
    Locale.setDefault(Locale.forLanguageTag('tr-TR'))

    when:
    List<Change> vehicles = generate(VEHICLES, dir).changes()
    List<Change> corners = corners(other).changes()

    then:
    snapshot('vehicle-service', vehicles)
    snapshot('corner-service', corners)

    cleanup:
    Locale.setDefault(locale)
  }

  def "a file that the pack does not carry is a mistake of its build, named by its name"() {
    when:
    DefaultPack.resource('wrapper/none')

    then:
    IllegalStateException e = thrown()
    e.message == 'the pack does not carry wrapper/none'
  }

  def "the generated Java is in the layout of google-java-format, which leaves it as it is: #name"() {
    when:
    List<Change> java = generate(spec, dir).changes().findAll { it.path().endsWith('.java') }

    then:
    !java.empty
    java.each { change ->
      String text = new String(change.after(), 'UTF-8')
      assert new Formatter().formatSourceAndFixImports(text) == text : change.path()
    }

    where:
    name              | spec
    'vehicle-service' | VEHICLES
    'corner-service'  | CORNERS
  }

  def "the first line of every generated file states its owner: #name"() {
    when:
    List<Change> changes = generate(spec, dir).changes()

    then:
    changes.findAll { !(it.path() in InitGenerator.WRAPPER) }.each { change ->
      String header = new String(change.after(), 'UTF-8').readLines()[0].replaceFirst(/^(\/\/|#|--|<!--) /, '')
      assert HEADERS[change.owner()].any { header.startsWith(it) } : change.path()
    }

    where:
    name              | spec
    'vehicle-service' | VEHICLES
    'corner-service'  | CORNERS
  }

  def "the same spec gives the same files, the state included: #name"() {
    when:
    Tree first = service(spec, dir)
    Tree second = service(spec, other)

    then:
    first.changes()*.path().contains(State.PATH)
    first.changes()*.path() == second.changes()*.path()
    first.changes().collect {
      it.after() as List
    } == second.changes().collect {
      it.after() as List
    }

    where:
    name              | spec
    'vehicle-service' | VEHICLES
    'corner-service'  | CORNERS
  }

  def "a second run on the generated service changes nothing: #name"() {
    given:
    if (migrations) {
      byHand(dir)
    }
    service(spec, dir).apply(State.PATH)
    DefaultPack pack = new DefaultPack()

    when:
    Tree again = Tree.at(dir)
    Generation.Result result = Generation.next(State.of(again, TIB), new Project(spec), [], pack, pack.entity(), TIB, again)

    then:
    again.changes().empty
    result == new Generation.Result([], [], [], [], [])

    where:
    name              | spec     | migrations
    'vehicle-service' | VEHICLES | false
    'corner-service'  | CORNERS  | true
  }

  def "the migrations of the service read back into its spec: #name"() {
    given:
    Tree tree = Tree.at(dir)
    new DefaultPack().entity().generate(new Project(spec), tree)

    when:
    String migrations = tree.list(EntityGenerator.MIGRATIONS).collect {
      tree.read(it).orElseThrow()
    }.join('\n')

    then:
    DdlReader.parse(migrations, spec) == readBack

    where:
    name              | spec     | readBack
    'vehicle-service' | VEHICLES | VEHICLES
    'corner-service'  | CORNERS  | CORNERS_READ_BACK
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
   * The service of the spec as two runs of tib make it, the first of a new service and the next of
   * its entities, staged in a tree at the directory, the state file included.
   */
  private static Tree service(Spec spec, Path dir) {
    Project project = new Project(spec)
    Tree tree = Tree.at(dir)
    DefaultPack pack = new DefaultPack()
    Generation.first(project, [], pack, pack.init(), TIB, tree)
    Generation.next(State.of(tree, TIB), project, [], pack, pack.entity(), TIB, tree)
    return tree
  }

  /** A migration by hand and a repeatable one, among the migrations of the service. */
  private static void byHand(Path dir) {
    Path migrations = Files.createDirectories(dir.resolve(EntityGenerator.MIGRATIONS))
    Files.writeString(migrations.resolve('V3__by_hand.sql'), '-- mine\n')
    Files.writeString(migrations.resolve('R__views.sql'), '-- repeatable\n')
  }

  /**
   * The entities of the corner spec, staged in a tree at the directory, which has a migration by
   * hand and a repeatable one.
   */
  private static Tree corners(Path dir) {
    byHand(dir)
    Tree tree = Tree.at(dir)
    new DefaultPack().entity().generate(new Project(CORNERS), tree)
    return tree
  }

  /**
   * The files are the ones of the snapshot, each as the snapshot says. What was generated is
   * written where the build asks for it, to be copied over the snapshot on purpose.
   */
  private static void snapshot(String name, List<Change> generated) {
    List<Change> changes = generated.findAll { !(it.path() in InitGenerator.WRAPPER) }
    Path actual = ACTUAL.resolve(name)
    actual.toFile().deleteDir()
    changes.each { change ->
      Path file = actual.resolve(change.path())
      Files.createDirectories(file.parent)
      Files.write(file, change.after())
    }
    Path expected = SNAPSHOTS.resolve(name)
    List<String> files = Files.walk(expected).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) }.collect {
        expected.relativize(it).toString().replace('\\', '/')
      }.sort()
    }
    assert files == changes*.path() : "the files are not the snapshot's; the generated files are in $actual"
    changes.each { change ->
      assert new String(change.after(), 'UTF-8') == Files.readString(expected.resolve(change.path())) : "${change.path()}; the generated files are in $actual"
    }
  }
}
