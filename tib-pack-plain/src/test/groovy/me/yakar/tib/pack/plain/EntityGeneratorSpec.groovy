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
import me.yakar.tib.generate.Generation
import me.yakar.tib.generate.Project
import me.yakar.tib.pack.Migration
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.state.State
import me.yakar.tib.tree.Owner
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class EntityGeneratorSpec extends Specification {

  /** A service with one entity. */
  static final Project A = project('entities:\n  - name: A\n')

  static final String BASE = 'src/main/java/com/example/s/'

  @TempDir
  Path dir

  def "an entity gets its classes, its migration, and the tests of the kinds of the spec: #tests"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("tests: $tests\nentities:\n  - name: A\n  - name: B\n    options: { dto: false }\n"), tree)
    List<String> written = tree.changes()*.path().findAll {
      it.startsWith('src/test/')
    }.collect {
      it.substring(it.lastIndexOf('/') + 1) - '.java'
    }

    then:
    written.toSorted() == expected.toSorted()
    tree.changes()*.path().findAll {
      it.startsWith('src/main/java/com/example/s/a/')
    }.collect {
      it - 'src/main/java/com/example/s/a/' - '.java'
    }.toSorted() ==
    ['A', 'ADto', 'AGenerated', 'AHandler', 'AMapper', 'ARepository', 'AService', 'AWiring']
    tree.changes()*.path().findAll {
      it.startsWith('src/main/java/com/example/s/b/')
    }.collect {
      it - 'src/main/java/com/example/s/b/' - '.java'
    }.toSorted() ==
    ['B', 'BGenerated', 'BRepository']

    where:
    tests                      | expected
    '[unit, integration, api]' | ['AApiTests', 'ARepositoryTests', 'ASamples', 'AServiceTests', 'BRepositoryTests', 'BSamples']
    '[integration]'            | ['ARepositoryTests', 'ASamples', 'BRepositoryTests', 'BSamples']
    '[unit, api]'              | ['AApiTests', 'ASamples', 'AServiceTests']
    '[unit]'                   | ['ASamples', 'AServiceTests']
    '[api]'                    | ['AApiTests', 'ASamples']
    '[]'                       | []
  }

  def "in #architecture, a file imports the class of the service that it names, and no other class of its name: #file"() {
    given:
    Tree tree = Tree.at(dir)
    String entities = 'entities:\n  - name: Instant\n    table: INSTANTS\n  - name: UUID\n    table: UUIDS\n    fields: [{ name: label, type: String, nullable: false }]\n  - name: Holder\n    audit: true\n    relations:\n      - { kind: manyToOne, target: Instant, nullable: false }\n      - { kind: manyToOne, target: UUID }\n'

    when:
    new EntityGenerator().generate(project("architecture: $architecture\n$entities"), tree)
    List<String> imports = tree.read("src/$file").orElseThrow().readLines().findAll {
      it.startsWith('import ') && it.endsWith(".$name;")
    }

    then:
    imports == imported

    where:
    architecture | file                                                                       | name      | imported
    'anemic'     | 'main/java/com/example/s/instant/InstantGenerated.java'                    | 'Instant' | []
    'anemic'     | 'main/java/com/example/s/instant/InstantRepository.java'                   | 'Instant' | []
    'anemic'     | 'main/java/com/example/s/instant/InstantDto.java'                          | 'Instant' | []
    'anemic'     | 'main/java/com/example/s/instant/InstantService.java'                      | 'Instant' | []
    'anemic'     | 'test/java/com/example/s/instant/InstantSamples.java'                      | 'Instant' | []
    'anemic'     | 'main/java/com/example/s/uuid/UUIDRepository.java'                         | 'UUID'    | []
    'anemic'     | 'main/java/com/example/s/uuid/UUIDDto.java'                                | 'UUID'    | []
    'anemic'     | 'test/java/com/example/s/uuid/UUIDServiceTests.java'                       | 'UUID'    | []
    'anemic'     | 'main/java/com/example/s/holder/HolderRepository.java'                     | 'Instant' | []
    'anemic'     | 'main/java/com/example/s/holder/HolderDto.java'                            | 'Instant' | ['import java.time.Instant;']
    'ddd'        | 'main/java/com/example/s/instant/domain/InstantRepository.java'            | 'Instant' | []
    'ddd'        | 'main/java/com/example/s/instant/web/InstantDto.java'                      | 'Instant' | []
    'ddd'        | 'main/java/com/example/s/instant/web/InstantHandler.java'                  | 'Instant' | ['import com.example.s.instant.domain.Instant;']
    'ddd'        | 'main/java/com/example/s/uuid/application/UUIDService.java'                | 'UUID'    | ['import com.example.s.uuid.domain.UUID;']
    'ddd'        | 'main/java/com/example/s/uuid/domain/UUIDRepository.java'                  | 'UUID'    | []
    'hexagonal'  | 'main/java/com/example/s/instant/adapter/out/persistence/InstantRepository.java' | 'Instant' | ['import com.example.s.instant.domain.Instant;']
    'hexagonal'  | 'main/java/com/example/s/instant/adapter/in/web/InstantDto.java'           | 'Instant' | []
    'hexagonal'  | 'main/java/com/example/s/uuid/adapter/out/persistence/UUIDRepository.java' | 'UUID'    | ['import com.example.s.uuid.domain.UUID;']
    'hexagonal'  | 'main/java/com/example/s/uuid/adapter/out/persistence/UUIDPersistenceAdapter.java' | 'UUID' | ['import com.example.s.uuid.domain.UUID;']
    'hexagonal'  | 'main/java/com/example/s/uuid/application/port/out/UUIDStore.java'         | 'UUID'    | ['import com.example.s.uuid.domain.UUID;']
  }

  def "the files of an entity have their owners: the ones that follow the spec are tib's"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(A, tree)
    Map<String, Owner> owners = tree.changes().collectEntries {
      [it.path() - 'src/main/java/com/example/s/' - 'src/test/java/com/example/s/', it.owner()]
    }

    then:
    [
      'a/AGenerated.java',
      'a/ARepository.java',
      'a/ADto.java',
      'a/AMapper.java',
      'a/ASamples.java',
      'Routes.java'
    ].every {
      owners[it] == Owner.GENERATOR
    }
    [
      'a/A.java',
      'a/AService.java',
      'a/AHandler.java',
      'a/AWiring.java',
      'a/AServiceTests.java',
      'a/ARepositoryTests.java',
      'a/AApiTests.java'
    ].every {
      owners[it] == Owner.DEVELOPER
    }
  }

  def "the routes put the handler that the wiring of an entity makes at its path, for the entities with a service and a handler only, and answer every other path of a service with an API as not found: #entities"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("entities:\n$entities"), tree)
    String routes = tree.read(BASE + 'Routes.java').orElseThrow()

    then:
    routes.contains('    route(server, "/api/as", AWiring.handler(transactions));\n') == a
    routes.contains('    route(server, "/api/bs", BWiring.handler(transactions));\n') == b
    ['AHandler', 'AService', 'ARepository', 'AMapper'].every { !routes.contains(it) }
    tree.read(BASE + 'b/BWiring.java').present == b
    routes.contains('getFilters().add(new ErrorHandler())') == (a || b)
    routes.contains('    route(server, "/", Routes::nothing);\n  }\n') == (a || b)
    routes.contains('  private static void nothing(HttpExchange exchange) {\n    throw new NotFoundException("There is nothing at " + exchange.getRequestURI().getPath());\n') == (a || b)
    !a || tree.read('src/test/java/com/example/s/a/AApiTests.java').orElseThrow().contains('  void aPathThatNoEntityHasIsNotFound() throws Exception {\n    HttpResponse<String> found = service.send("GET", "/api");\n\n    assertEquals(404, found.statusCode());\n')

    where:
    entities                                                            | a     | b
    '  - name: A\n  - name: B\n'                                        | true  | true
    '  - name: A\n  - name: B\n    options: { mapper: false }\n'        | true  | false
    '  - name: A\n    options: { dto: false }\n'                        | false | false
  }

  def "the first entity with an API brings the support of the API, and the first one in pages the page, once: #entities"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("entities:\n$entities"), tree)
    List<String> paths = tree.changes()*.path()

    then:
    ['NotFoundException', 'BadRequestException', 'ErrorHandler', 'Http'].every {
      paths.contains("${BASE}${it}.java" as String) == api
    }
    paths.contains("${BASE}Page.java" as String) == page
    tree.changes().findAll {
      it.path().startsWith(BASE) && !(it.path() - BASE).contains('/') && !it.path().endsWith('Routes.java')
    }.every {
      it.owner() == Owner.DEVELOPER
    }

    where:
    entities                                                                    | api   | page
    '  - name: A\n'                                                             | true  | true
    '  - name: A\n    options: { pagination: false }\n'                         | true  | false
    '  - name: A\n    options: { mapper: false }\n'                             | false | true
    '  - name: A\n    options: { mapper: false, pagination: false }\n'          | false | false
  }

  def "the wiring of an entity, which is the developer's, makes its handler with what the handler works on: #architecture"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("architecture: $architecture\nentities:\n  - name: A\n"), tree)
    String wiring = tree.read(BASE + 'a/AWiring.java').orElseThrow()

    then:
    wiring.startsWith('// Written by tib when the entity was created; it is yours from then on.\npackage com.example.s.a;\n')
    wiring.contains('  public static HttpHandler handler(Transactions transactions) {\n    ARepository repository = new ARepository(transactions);\n')
    wiring.contains(made)

    where:
    architecture | made
    'anemic'     | '    AService service = new AService(transactions, repository, new AMapper());\n    return new AHandler(service);\n'
    'ddd'        | '    AService service = new AService(transactions, repository);\n    return new AHandler(service, new AMapper());\n'
    'hexagonal'  | '    AStore store = new APersistenceAdapter(transactions, repository);\n    AService service = new AService(transactions, store);\n    return new AHandler(service, new AWebMapper());\n'
  }

  def "a repository has nothing that only the tests call: they empty the tables through the database of the tests, and count the rows of a list in pages"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project('entities:\n  - name: B\n    options: { pagination: false }\n  - name: A\n    relations:\n      - { kind: manyToOne, target: B, nullable: false }\n'), tree)
    String a = tree.read(BASE + 'a/ARepository.java').orElseThrow()
    String b = tree.read(BASE + 'b/BRepository.java').orElseThrow()
    String tests = BASE.replace('/main/', '/test/')

    then:
    !a.contains('deleteAll') && !b.contains('deleteAll')
    a.contains('  public long count() throws SQLException {\n')
    !b.contains('count()')
    tree.read(tests + 'a/ARepositoryTests.java').orElseThrow().contains('  void deleteAll() {\n    TestDatabase.clear("A", "B");\n  }\n')
    tree.read(tests + 'a/AApiTests.java').orElseThrow().contains('  void deleteAll() {\n    TestDatabase.clear("A", "B");\n  }\n')
    tree.read(tests + 'b/BRepositoryTests.java').orElseThrow().contains('    List<B> found = transactions.run(() -> bRepository.findAll());\n    assertEquals(1, found.size());\n')
    !tree.read(tests + 'b/BApiTests.java').orElseThrow().contains('Transactions')
  }

  def "the tests of the API declare their fields before their methods"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project('entities:\n  - name: B\n  - name: A\n    relations:\n      - { kind: manyToOne, target: B, nullable: false }\n'), tree)
    String tests = tree.read('src/test/java/com/example/s/a/AApiTests.java').orElseThrow()

    then:
    tests.contains('  private final BRepository bRepository = new BRepository(transactions);\n\n  private B b;\n\n  @BeforeEach\n')
    tests.contains('    b = transactions.run(() -> bRepository.insert(BSamples.b().build()));\n  }\n\n  @BeforeAll\n')
  }

  def "a service learns from the update and from the delete whether the row was there, without a query before the delete: #architecture"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project("architecture: $architecture\nentities:\n  - name: A\n"), tree)
    String service = tree.read(BASE + main).orElseThrow()
    String tests = tree.read(BASE.replace('/main/', '/test/') + main.replace('.java', 'Tests.java')).orElseThrow()

    then:
    service.contains("          if (!${port}.update(a)) {\n            throw notFound(id);\n          }\n")
    service.contains("          if (!${port}.delete(id)) {\n            throw notFound(id);\n          }\n")
    !service.contains('find(id).getId()')
    tests.contains("  void deletingAnUnknownAIsNotFound()")
    tests.contains("    when(${port}.delete(ID)).thenReturn(false);\n\n    assertThrows(NotFoundException.class, () -> service.delete(ID));\n")
    tests.contains("  void updatingAnAThatIsGoneIsNotFound()")

    where:
    architecture | main                          | port
    'anemic'     | 'a/AService.java'             | 'repository'
    'ddd'        | 'a/application/AService.java' | 'repository'
    'hexagonal'  | 'a/application/AService.java' | 'store'
  }

  def "the store of the hexagonal architecture says whether the row was there, as the repository does"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project('architecture: hexagonal\nentities:\n  - name: A\n'), tree)
    String store = tree.read(BASE + 'a/application/port/out/AStore.java').orElseThrow()
    String adapter = tree.read(BASE + 'a/adapter/out/persistence/APersistenceAdapter.java').orElseThrow()

    then:
    store.contains('  boolean update(A a);\n')
    store.contains('  boolean delete(Long id);\n')
    adapter.contains('  public boolean update(A a) {\n    return transactions.run(() -> repository.update(a));\n')
    adapter.contains('  public boolean delete(Long id) {\n    return transactions.run(() -> repository.delete(id));\n')
  }

  def "a value that does not fit its column is a conflict, as a broken constraint is, and the tests of the API of an entity that can have one show it"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project('entities:\n  - name: A\n    fields:\n      - { name: price, type: BigDecimal, precision: 12, scale: 2 }\n  - name: B\n'), tree)
    String tests = tree.read('src/test/java/com/example/s/a/AApiTests.java').orElseThrow()

    then:
    tree.read(BASE + 'ErrorHandler.java').orElseThrow().contains('if (state != null && (state.startsWith(DATA) || state.startsWith(CONSTRAINT))) {\n        Http.problem(exchange, 409, ')
    tests.contains('  void refusesAnAWhoseValueDoesNotFitItsColumn() throws Exception {\n    ADto tooLarge = ASamples.dto().price(BigDecimal.TEN.pow(10)).build();\n')
    !tree.read('src/test/java/com/example/s/b/BApiTests.java').orElseThrow().contains('DoesNotFitItsColumn')
  }

  def "a body that is not the JSON of the DTO is a bad request, which says that it cannot be read"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(A, tree)

    then:
    tree.read(BASE + 'Http.java').orElseThrow().contains('throw new BadRequestException(\n          "The body of the request cannot be read: " + e.getOriginalMessage());\n')
  }

  def "the support of the API is written once: there #there, written before #recorded"() {
    given:
    String handler = BASE + 'ErrorHandler.java'
    if (there) {
      Files.createDirectories(dir.resolve(handler).parent)
      Files.writeString(dir.resolve(handler), 'mine\n')
    }
    Tree tree = Tree.at(dir)
    if (recorded) {
      tree.writtenBefore([handler])
    }

    when:
    new EntityGenerator().generate(A, tree)

    then:
    !tree.changes()*.path().contains(handler)

    where:
    there | recorded
    true  | false
    false | true
    true  | true
  }

  def "the sequence of a sequence identifier advances by one, as the repository takes an identifier for every row"() {
    given:
    Tree tree = Tree.at(dir)

    when:
    new EntityGenerator().generate(project('entities:\n  - name: A\n    id: { strategy: sequence }\n'), tree)

    then:
    tree.read("${Migration.DIRECTORY}/V1__create_a.sql").orElseThrow().contains('CREATE SEQUENCE A_SEQ START WITH 1 INCREMENT BY 1;\n')
    tree.read(BASE + 'a/ARepository.java').orElseThrow().contains("nextval('A_SEQ')")
  }

  def "the migration of a table lasts: a run whose spec no longer has the table does not delete it"() {
    given:
    PlainPack pack = new PlainPack()
    Tree tree = Tree.at(dir)
    new Generation(pack, '0.0.0', tree).first(A, [])
    new Generation(pack, '0.0.0', tree).next(State.of(tree, '0.0.0'), A, [])
    tree.apply(State.PATH)

    when:
    Tree later = Tree.at(dir)
    new Generation(pack, '0.0.0', later).next(State.of(later, '0.0.0'), project(''), [])

    then:
    Files.readString(dir.resolve("${Migration.DIRECTORY}/V1__create_a.sql")).endsWith('CREATE TABLE A (\n  ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY\n);\n')
    List<String> deleted = later.changes().findAll { it.deletes() }*.path()
    deleted.contains(BASE + 'a/A.java')
    !deleted.contains("${Migration.DIRECTORY}/V1__create_a.sql" as String)
    later.read(BASE + 'Routes.java').orElseThrow().contains('public static void add(HttpServer server, Transactions transactions) {}\n')
  }

  private static Project project(String yaml) {
    new Project(SpecReader.parse("tib: 1\nservice: s\npack: plain\n$yaml"))
  }
}
