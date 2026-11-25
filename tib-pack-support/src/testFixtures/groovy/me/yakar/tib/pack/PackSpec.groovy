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
import me.yakar.tib.state.State
import me.yakar.tib.tree.Change
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

/**
 * What every pack keeps, whatever its stack, checked on the services of {@link Services}: each is
 * generated as its snapshot says, on any machine; every file states its owner; the same spec gives
 * the same files; a second run changes nothing. The specification of a pack extends this one, gives
 * its pack, and adds what is the pack's own.
 */
abstract class PackSpec extends Specification {

  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))
  static final Path ACTUAL = Path.of(System.getProperty('tib.snapshots.actual'))

  /** The root of this build, whose Gradle wrapper a pack carries. */
  static final Path BUILD = Path.of(System.getProperty('tib.root'))

  /** The version of tib that the build makes. */
  static final String VERSION = System.getProperty('tib.version')

  /** The version of the tib that runs in a test. */
  static final String TIB = '0.0.0'

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

  /** The pack under test. */
  abstract Pack pack()

  def "the pack has the version of tib, and the engine finds it by its name"() {
    given:
    Pack pack = pack()

    expect:
    pack.version() == VERSION
    Packs.names() == [pack.name()]
    Packs.find(pack.name()).class == pack.class
    Packs.find("${pack.name()}@$VERSION").class == pack.class
  }

  def "a new service comes with the Gradle wrapper of this build, written once, and gradlew is run"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    pack().init().generate(new Project(Services.vehicles(pack())), tree)
    List<Change> wrapper = tree.changes().findAll { it.path() in GradleWrapper.FILES }

    then: 'the files are Gradle\'s, as they are, without a header of tib\'s'
    wrapper*.path() == GradleWrapper.FILES.toSorted()
    wrapper.every { it.owner() == Owner.DEVELOPER && it.before() == null }
    wrapper.findAll { it.executable() }*.path() == ['gradlew']
    wrapper.each { assert it.after() == Files.readAllBytes(BUILD.resolve(it.path())) : it.path() }
  }

  def "generates the service as its snapshot says: #name"() {
    when:
    List<Change> changes = generate(name, dir).changes()

    then:
    snapshot(name, changes)

    where:
    name << Services.NAMES
  }

  def "a machine in Turkish, where the upper case of i is not I, generates the same service: #name"() {
    given:
    Locale locale = Locale.default
    Locale.setDefault(Locale.forLanguageTag('tr-TR'))

    when:
    List<Change> changes = generate(name, dir).changes()

    then:
    matches(name, changes)

    cleanup:
    Locale.setDefault(locale)

    where:
    name << [Services.VEHICLES, 'corner-service']
  }

  def "the generated Java is in the layout of google-java-format, which leaves it as it is: #name"() {
    when:
    List<Change> java = generate(name, dir).changes().findAll { it.path().endsWith('.java') }

    then:
    !java.empty
    java.each { change ->
      String text = new String(change.after(), 'UTF-8')
      assert new Formatter().formatSourceAndFixImports(text) == text : change.path()
    }

    where:
    name << Services.NAMES
  }

  def "the first line of every generated file states its owner: #name"() {
    when:
    List<Change> changes = generate(name, dir).changes()

    then:
    snapshotted(changes).each { change ->
      String header = new String(change.after(), 'UTF-8').readLines()[0].replaceFirst(/^(\/\/|#|--|<!--) /, '')
      assert HEADERS[change.owner()].any { header.startsWith(it) } : change.path()
    }

    where:
    name << Services.NAMES
  }

  def "every generated Java file is in the directory of its package: #name"() {
    when:
    List<Change> java = generate(name, dir).changes().findAll { it.path().endsWith('.java') }

    then:
    !java.empty
    java.each { change ->
      String declared = new String(change.after(), 'UTF-8').readLines().find {
        it.startsWith('package ')
      }
      String directory = change.path().replaceFirst(/^src\/(main|test)\/java\//, '').replaceFirst(/\/[^\/]+$/, '')
      assert declared == "package ${directory.replace('/', '.')};" : change.path()
    }

    where:
    name << Services.NAMES
  }

  def "the notes for agents name every header: #header"() {
    given:
    Tree tree = Tree.at(dir)
    pack().init().generate(new Project(Services.vehicles(pack())), tree)

    expect: 'what a header says after "Written by tib", up to its first mark'
    tree.read('AGENTS.md').orElseThrow().replaceAll(/\s+/, ' ').contains((header - 'Written by tib ').split(/[;.]/)[0])

    where:
    header << HEADERS.values().flatten()
  }

  def "the same spec gives the same files, the state included: #name"() {
    when:
    Tree first = service(name, dir)
    Tree second = service(name, other)

    then:
    first.changes()*.path().contains(State.PATH)
    first.changes()*.path() == second.changes()*.path()
    first.changes().collect {
      it.after() as List
    } == second.changes().collect {
      it.after() as List
    }

    where:
    name << Services.NAMES
  }

  def "a second run on the generated service changes nothing: #name"() {
    given:
    service(name, dir).apply(State.PATH)

    when:
    Tree again = Tree.at(dir)
    Generation.Result result = new Generation(pack(), TIB, again).next(State.of(again, TIB), new Project(Services.named(name, pack())), [])

    then:
    again.changes().empty
    result == new Generation.Result([], [], [], [], [])

    where:
    name << Services.NAMES
  }

  def "the migrations of the service read back into its spec: #name"() {
    given:
    Tree tree = Tree.at(dir)
    pack().entity().generate(new Project(Services.named(name, pack())), tree)

    when:
    String migrations = tree.list(Migration.DIRECTORY).collect {
      tree.read(it).orElseThrow()
    }.join('\n')

    then:
    DdlReader.parse(migrations, Services.named(name, pack())) == Services.readBack(name, pack())

    where:
    name << [Services.VEHICLES, 'corner-service']
  }

  /**
   * The service of the name with its entities, staged in a tree at the directory. A corner service
   * has a migration by hand and a repeatable one there.
   */
  protected Tree generate(String name, Path dir) {
    byHand(name, dir)
    Project project = new Project(Services.named(name, pack()))
    Tree tree = Tree.at(dir)
    pack().init().generate(project, tree)
    pack().entity().generate(project, tree)
    return tree
  }

  /**
   * The service of the name as two runs of tib make it, the first of a new service and the next of
   * its entities, staged in a tree at the directory, the state file included. A corner service has
   * a migration by hand and a repeatable one there.
   */
  protected Tree service(String name, Path dir) {
    byHand(name, dir)
    Project project = new Project(Services.named(name, pack()))
    Tree tree = Tree.at(dir)
    Generation generation = new Generation(pack(), TIB, tree)
    generation.first(project, [])
    generation.next(State.of(tree, TIB), project, [])
    return tree
  }

  /** A migration by hand and a repeatable one, among the migrations of a corner service. */
  protected static void byHand(String name, Path dir) {
    if (name in Services.CORNERS.keySet()) {
      Path migrations = Files.createDirectories(dir.resolve(Migration.DIRECTORY))
      Files.writeString(migrations.resolve('V3__by_hand.sql'), '-- mine\n')
      Files.writeString(migrations.resolve('R__views.sql'), '-- repeatable\n')
    }
  }

  /**
   * Writes what was generated where the build asks for it, to be copied over the snapshot on
   * purpose, and checks that it is what the snapshot holds.
   */
  protected static void snapshot(String name, List<Change> generated) {
    Path actual = ACTUAL.resolve(name)
    actual.toFile().deleteDir()
    snapshotted(generated).each { change ->
      Path file = actual.resolve(change.path())
      Files.createDirectories(file.parent)
      Files.write(file, change.after())
    }
    matches(name, generated)
  }

  /** The files are the ones of the snapshot, each as the snapshot says. */
  protected static void matches(String name, List<Change> generated) {
    List<Change> changes = snapshotted(generated)
    Path expected = SNAPSHOTS.resolve(name)
    List<String> files = Files.walk(expected).withCloseable { paths ->
      paths.filter { Files.isRegularFile(it) }.collect {
        expected.relativize(it).toString().replace('\\', '/')
      }.sort()
    }
    assert files == changes*.path() : "the files are not the ones of the snapshot $name"
    changes.each { change ->
      assert new String(change.after(), 'UTF-8') == Files.readString(expected.resolve(change.path())) : "${change.path()} is not as the snapshot $name says"
    }
  }

  /** What a snapshot holds of the generated files: all but the Gradle wrapper. */
  protected static List<Change> snapshotted(List<Change> generated) {
    generated.findAll { !(it.path() in GradleWrapper.FILES) }
  }
}
