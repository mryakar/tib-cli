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

import spock.lang.Specification

class EntityModelSpec extends Specification {

  def "the article of the name is #article: #name"() {
    expect:
    models("entities:\n  - name: $name\n")[name].article() == article

    where:
    name    | article
    'Tag'   | 'A'
    'Item'  | 'An'
  }

  def "the relations that a sample sets are the ones that are not nullable, but for one to itself"() {
    given:
    Map<String, EntityModel> models = models('''\
      entities:
        - name: B
        - name: C
        - name: A
          relations:
            - { kind: manyToOne, target: B, nullable: false }
            - { kind: manyToOne, target: B, field: other, nullable: false }
            - { kind: manyToOne, target: A, field: parent, nullable: false }
            - { kind: manyToOne, target: C }
      '''.stripIndent())

    expect:
    models.A.requiredRelations()*.field() == ['b', 'other']
  }

  def "the names that tib adds to a table follow its case: #table"() {
    given:
    EntityModel model = models("""\
      entities:
        - name: B
        - name: A
          table: $table
          id: { strategy: sequence }
          audit: true
          relations:
            - { kind: manyToOne, target: B, column: $column }
      """.stripIndent()).A

    expect:
    [model.sequence(), model.createdAt(), model.updatedAt(), model.index(model.relations()[0])] == names

    where:
    table      | column   | names
    'VEHICLES' | 'B_ID'   | ['VEHICLES_SEQ', 'CREATED_AT', 'UPDATED_AT', 'VEHICLES_B_ID_IDX']
    'Vehicles' | 'b_id'   | ['Vehicles_SEQ', 'CREATED_AT', 'UPDATED_AT', 'Vehicles_b_id_IDX']
    'vehicles' | 'b_id'   | ['vehicles_seq', 'created_at', 'updated_at', 'vehicles_b_id_idx']
    'v_2'      | 'B_ID'   | ['v_2_seq', 'created_at', 'updated_at', 'v_2_B_ID_idx']
  }

  def "the names of an entity come from its name"() {
    given:
    EntityModel model = models('entities:\n  - name: Vehicle\n').Vehicle

    expect:
    [model.basePackage(), model.name(), model.variable(), model.plural(), model.path(), model.table()] ==
    ['com.example.s', 'Vehicle', 'vehicle', 'vehicles', '/api/vehicles', 'VEHICLE']
  }

  def "the packages and the classes of an entity follow the architecture: #architecture"() {
    given:
    EntityModel model = models("architecture: $architecture\nentities:\n  - name: Vehicle\n").Vehicle
    String entity = 'com.example.s.vehicle'

    expect:
    model.packageName() == entity
    [
      model.domainPackage(),
      model.applicationPackage(),
      model.webPackage(),
      model.persistencePackage(),
      model.useCasePackage(),
      model.storePackage()
    ] ==
    packages.collect { it ? "$entity.$it" : entity }
    [model.hexagonal(), model.refersById()] == [hexagonal, refersById]
    [model.jpaEntity(), model.repository(), model.webMapper()] == classes

    where:
    architecture | packages                                                                                               | hexagonal | refersById | classes
    'anemic'     | ['', '', '', '', 'port.in', 'port.out']                                                                 | false     | false      | ['Vehicle', 'VehicleRepository', 'VehicleMapper']
    'ddd'        | ['domain', 'application', 'web', 'domain', 'application.port.in', 'application.port.out']              | false     | true       | ['Vehicle', 'VehicleRepository', 'VehicleMapper']
    'hexagonal'  | [
      'domain',
      'application',
      'adapter.in.web',
      'adapter.out.persistence',
      'application.port.in',
      'application.port.out'
    ] | true | true | ['VehicleEntity', 'VehicleJpaRepository', 'VehicleWebMapper']
  }

  def "a type is imported into a file of another package, and the directories of a package are under the sources and the tests"() {
    expect:
    EntityModel.importOf('a.b', 'a.b', 'C') == ''
    EntityModel.importOf('a', 'a.b', 'C') == 'import a.b.C;'
    EntityModel.directory('a.b') == 'src/main/java/a/b'
    EntityModel.testDirectory('a.b') == 'src/test/java/a/b'
  }

  def "the identifier is mapped by its strategy: #id"() {
    given:
    EntityModel model = models("entities:\n  - name: A\n    id: $id\n").A

    expect:
    [
      model.idField(),
      model.idColumn(),
      model.idType(),
      model.idSqlType(),
      model.uuidId(),
      model.idSequence()
    ] == identifier
    model.idAnnotations() == annotations
    model.columns() == [column]
    model.sequenceIncrement() == 50

    where:
    id                       | identifier                                       | annotations                                                                                                                                                       | column
    '{}'                     | ['id', 'ID', 'Long', 'BIGINT', false, false]     | ['@Id', '@GeneratedValue(strategy = GenerationType.IDENTITY)']                                                                                                    | 'ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY'
    '{ strategy: sequence }' | ['id', 'ID', 'Long', 'BIGINT', false, true]      | [
      '@Id',
      '@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "a")',
      '@SequenceGenerator(name = "a", sequenceName = "A_SEQ", allocationSize = 50)'
    ] | 'ID BIGINT PRIMARY KEY'
    '{ type: UUID, field: code }' | ['code', 'CODE', 'UUID', 'UUID', true, false] | ['@Id', '@GeneratedValue(strategy = GenerationType.UUID)']                                                                                                       | 'CODE UUID PRIMARY KEY'
  }

  def "the options of an entity say what it gets: #options"() {
    given:
    EntityModel model = models("entities:\n  - name: A\n    options: $options\n").A

    expect:
    [model.dto(), model.layer(), model.paginated()] == gets

    where:
    options                        | gets
    '{}'                           | [true, true, true]
    '{ mapper: false }'            | [true, false, true]
    '{ dto: false, mapper: false }' | [false, false, true]
    '{ pagination: false }'        | [true, true, false]
  }

  def "the members of an entity make its DTO, its mapper and its table, in the order of the spec: #architecture"() {
    given:
    Map<String, EntityModel> models = models("""\
      architecture: $architecture
      entities:
        - name: B
        - name: C
          id: { type: UUID }
        - name: A
          audit: true
          fields: [{ name: label, type: String, nullable: false }]
          relations:
            - { kind: manyToOne, target: B }
            - { kind: oneToOne, target: C, nullable: false }
            - { kind: manyToOne, target: A, field: parent }
      """.stripIndent())
    EntityModel model = models.A

    expect:
    model.audited()
    model.fields()*.name() == ['label']
    model.relations()*.field() == ['b', 'c', 'parent']
    model.targets()*.name() == ['B', 'C', 'A']
    model.otherTargets()*.name() == ['B', 'C']
    model.dtoComponents() == 'Long id, @NotNull @Size(max = 255) String label, Long bId, @NotNull UUID cId, Long parentId, Instant createdAt, Instant updatedAt'
    model.repositoryParameters() == 'BRepository bs, CRepository cs, ARepository as'
    model.dtoArguments() == arguments
    model.indexed()*.field() == ['b', 'parent']
    model.columns() == [
      'ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY',
      'LABEL VARCHAR(255) NOT NULL',
      'B_ID BIGINT REFERENCES B (ID)',
      'C_ID UUID NOT NULL UNIQUE REFERENCES C (ID)',
      'PARENT_ID BIGINT REFERENCES A (ID)',
      'CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL',
      'UPDATED_AT TIMESTAMP WITH TIME ZONE NOT NULL'
    ]
    !models.B.audited()
    models.B.dtoComponents() == 'Long id'
    models.B.dtoArguments() == 'b.getId()'
    models.B.repositoryParameters() == ''

    where:
    architecture | arguments
    'anemic'     | 'a.getId(), a.getLabel(), a.getB() == null ? null : a.getB().getId(), a.getC() == null ? null : a.getC().getId(), a.getParent() == null ? null : a.getParent().getId(), a.getCreatedAt(), a.getUpdatedAt()'
    'ddd'        | 'a.getId(), a.getLabel(), a.getBId(), a.getCId(), a.getParentId(), a.getCreatedAt(), a.getUpdatedAt()'
  }

  /** The models of the entities of the spec, by name. */
  private static Map<String, EntityModel> models(String entities) {
    Models.of(entities)
  }
}
