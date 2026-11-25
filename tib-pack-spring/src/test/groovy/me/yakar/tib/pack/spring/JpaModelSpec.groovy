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
package me.yakar.tib.pack.spring

import me.yakar.tib.generate.Project
import me.yakar.tib.pack.EntityModel
import me.yakar.tib.spec.SpecReader
import spock.lang.Specification

class JpaModelSpec extends Specification {

  def "the class of the table and the repository are named by the architecture: #architecture"() {
    given:
    Map<String, EntityModel> models = models("architecture: $architecture\nentities:\n  - name: Depot\n  - name: Vehicle\n")
    JpaModel jpa = new JpaModel(models.Vehicle)

    expect:
    [jpa.entityClass(), jpa.repository(), jpa.repository(models.Depot)] == classes

    where:
    architecture | classes
    'anemic'     | ['Vehicle', 'VehicleRepository', 'DepotRepository']
    'ddd'        | ['Vehicle', 'VehicleRepository', 'DepotRepository']
    'hexagonal'  | ['VehicleEntity', 'VehicleJpaRepository', 'DepotJpaRepository']
  }

  def "the identifier is mapped by its strategy: #id"() {
    expect:
    new JpaModel(models("entities:\n  - name: A\n    id: $id\n").A).idAnnotations() == annotations

    where:
    id                            | annotations
    '{}'                          | ['@Id', '@GeneratedValue(strategy = GenerationType.IDENTITY)']
    '{ strategy: sequence }'      | [
      '@Id',
      '@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "a")',
      '@SequenceGenerator(name = "a", sequenceName = "A_SEQ", allocationSize = 50)'
    ]
    '{ type: UUID, field: code }' | ['@Id', '@GeneratedValue(strategy = GenerationType.UUID)']
  }

  def "the column of a field says what its spec says: #field"() {
    given:
    EntityModel model = models("entities:\n  - name: A\n    fields: [$field]\n").A

    expect:
    new JpaModel(model).column(model.fields()[0]) == column

    where:
    field                                                                  | column
    '{ name: v, type: String }'                                            | '@Column(name = "V", length = 255)'
    '{ name: v, type: String, length: 20, nullable: false, unique: true }' | '@Column(name = "V", nullable = false, unique = true, length = 20)'
    '{ name: v, type: BigDecimal, precision: 12, scale: 2 }'               | '@Column(name = "V", precision = 12, scale = 2)'
    '{ name: v, type: BigDecimal, precision: 12 }'                         | '@Column(name = "V", precision = 12)'
    '{ name: v, type: Integer, column: QTY, nullable: false }'             | '@Column(name = "QTY", nullable = false)'
  }

  def "a relation is mapped by its kind, and its column says what its spec says: #relation"() {
    given:
    EntityModel model = models("entities:\n  - name: B\n  - name: A\n    relations: [$relation]\n").A
    JpaModel jpa = new JpaModel(model)

    expect:
    [
      jpa.annotation(model.relations()[0]),
      jpa.mapping(model.relations()[0]),
      jpa.joinColumn(model.relations()[0]),
      jpa.column(model.relations()[0])
    ] == annotations

    where:
    relation                                                       | annotations
    '{ kind: manyToOne, target: B }'                               | [
      'ManyToOne',
      '@ManyToOne(fetch = FetchType.LAZY)',
      '@JoinColumn(name = "B_ID")',
      '@Column(name = "B_ID")'
    ]
    '{ kind: oneToOne, target: B, field: other, nullable: false }' | [
      'OneToOne',
      '@OneToOne(fetch = FetchType.LAZY, optional = false)',
      '@JoinColumn(name = "OTHER_ID", nullable = false, unique = true)',
      '@Column(name = "OTHER_ID", nullable = false, unique = true)'
    ]
  }

  def "the mapper of an entity takes the repositories of the targets of its relations, once each"() {
    given:
    Map<String, EntityModel> models = models('''\
      entities:
        - name: B
        - name: C
        - name: A
          relations:
            - { kind: manyToOne, target: B }
            - { kind: oneToOne, target: C, nullable: false }
            - { kind: manyToOne, target: B, field: other }
            - { kind: manyToOne, target: A, field: parent }
      '''.stripIndent())

    expect:
    new JpaModel(models.A).repositoryParameters() == 'BRepository bs, CRepository cs, ARepository as'
    new JpaModel(models.B).repositoryParameters() == ''
  }

  /** The models of the entities of the spec of a service, by name. */
  private static Map<String, EntityModel> models(String spec) {
    EntityModel.models(new Project(SpecReader.parse("tib: 1\nservice: s\n$spec")))
  }
}
