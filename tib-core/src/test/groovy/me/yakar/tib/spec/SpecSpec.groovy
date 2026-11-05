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
package me.yakar.tib.spec

import spock.lang.Specification

class SpecSpec extends Specification {

  def "a spec with a problem is refused: #problem"() {
    when:
    SpecReader.parse(yaml)

    then:
    SpecException e = thrown()
    e.problems() == [problem]
    e.message == "the spec has a problem: $problem"

    where:
    yaml                                                                        | problem
    'service: s'                                                                | 'tib: required'
    'tib: 0\nservice: s'                                                        | 'tib: 0 is below 1'
    'tib: 2\nservice: s'                                                        | 'tib: 2 is above 1'
    'tib: 1\nservice: Vehicle Service'                                          | "service: 'Vehicle Service' does not match ${Names.SERVICE}"
    'tib: 1\nservice: s\npack: Default'                                         | "pack: 'Default' does not match ${Names.PACK}"
    'tib: 1\nservice: s\ngroup: Com.Example'                                     | "group: 'Com.Example' does not match ${Names.GROUP}"
    'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n  - name: Vehicle'       | "entities[1].name: 'Vehicle' is already an entity"
    'tib: 1\nservice: s\nentities:\n  -\n'                                      | 'entities[0]: missing'
    entity('relations:\n      - { kind: manyToOne, target: Depot }')            | "entities[0].relations[0].target: there is no entity 'Depot'"
    field('{ name: id, type: Long }')                                           | "entities[0].fields[0].name: 'id' is already a member"
    entity('fields:\n      - { name: self, type: Long }\n    relations:\n      - { kind: oneToOne, target: Vehicle, field: self }') | "entities[0].relations[0].field: 'self' is already a member"
    entity('id: { type: UUID, strategy: identity }')                            | 'entities[0].id: the strategy identity does not fit the type UUID'
    field('{ name: n, type: Integer, length: 3 }')                              | 'entities[0].fields[0].length: only a String has a length'
    field('{ name: n, type: String, precision: 3 }')                            | 'entities[0].fields[0]: only a BigDecimal has a precision and a scale'
    field('{ name: n, type: BigDecimal, precision: 3, scale: 4 }')              | 'entities[0].fields[0].scale: the scale is above the precision'
    field('{ name: n, type: String, length: 0 }')                               | 'entities[0].fields[0].length: 0 is below 1'
    field('{ type: String }')                                                   | 'entities[0].fields[0].name: required'
    field('{ name: n, type: String, scale: 2 }')                                | 'entities[0].fields[0]: only a BigDecimal has a precision and a scale'
    'tib: 1\nservice: s\nentities:\n  - { table: X }'                            | 'entities[0].name: required'
    'tib: 1\nservice: s\nentities:\n  - { audit: true }'                         | 'entities[0].name: required'
    entity('relations:\n      - { kind: manyToOne }')                           | 'entities[0].relations[0].target: required'
    field('{ name: n, type: String, column: "plate number" }')                  | "entities[0].fields[0].column: 'plate number' does not match ${Names.SQL}"
    field('{ name: key, type: Long, column: id }')                              | "entities[0].fields[0].column: 'id' is already a column"
    entity('fields:\n      - { name: a, type: Long }\n      - { name: b, type: Long, column: A }') | "entities[0].fields[1].column: 'A' is already a column"
    entity('audit: true\n    fields:\n      - { name: created, type: Instant, column: created_at }') | "entities[0].fields[0].column: 'created_at' is already a column"
    entity('audit: true\n    fields:\n      - { name: createdAt, type: Instant, column: CREATION }') | "entities[0].fields[0].name: 'createdAt' is already a member of an audited entity"
    entity('audit: true\n    relations:\n      - { kind: manyToOne, target: Vehicle, field: updatedAt }') | "entities[0].relations[0].field: 'updatedAt' is already a member of an audited entity"
    entity('audit: true\n    id: { field: createdAt }')                         | "entities[0].id.field: 'createdAt' is already a member of an audited entity"
    entity('id: { field: code }\n    fields:\n      - { name: id, type: Long }') | "entities[0].fields[0].name: 'id' is already a member: the accessors of the identifier are getId and setId"
    entity('fields:\n      - { name: vehicleId, type: Long, column: OTHER }\n    relations:\n      - { kind: manyToOne, target: Vehicle }') | "entities[0].relations[0].field: 'vehicleId', which holds the identifier of the target, is already a member"
    'tib: 1\nservice: s\nentities:\n  - name: A\n    id: { field: code }\n    relations:\n      - { kind: manyToOne, target: B, field: bCode }\n      - { kind: manyToOne, target: A, field: b }\n  - name: B' | "entities[0].relations[1].field: 'bCode', which holds the identifier of the target, is already a member"
    entity('fields:\n      - { name: selfId, type: Long }\n    relations:\n      - { kind: manyToOne, target: Vehicle }\n      - { kind: manyToOne, target: Vehicle, field: other, column: SELF_ID }') | "entities[0].relations[1].column: 'SELF_ID' is already a column"
    'tib: 1\nservice: s\nentities:\n  - name: Car\n    table: VEHICLE\n  - name: Vehicle' | "entities[1].table: 'VEHICLE' is already a table"
    'tib: 1\nservice: int'                                                      | "service: 'int' makes the package 'int', which is a Java keyword"
    'tib: 1\nservice: s\ngroup: com.new'                                        | "group: 'com.new' has 'new', which is a Java keyword"
    'tib: 1\nservice: s\nentities:\n  - name: Class'                           | "entities[0].name: 'Class' makes the package 'class', which is a Java keyword"
    'tib: 1\nservice: s\nentities:\n  - name: User'                            | "entities[0].table: 'USER' is a reserved word in SQL; give the entity a table"
    entity('id: { field: int }')                                                | "entities[0].id.field: 'int' is a Java keyword"
    entity('id: { field: user }')                                               | "entities[0].id.field: the column 'USER' is a reserved word in SQL; give the identifier another field"
    field('{ name: class, type: String }')                                      | "entities[0].fields[0].name: 'class' is a Java keyword"
    field('{ name: order, type: Integer }')                                     | "entities[0].fields[0].column: 'ORDER' is a reserved word in SQL; give the field a column"
    entity('relations:\n      - { kind: manyToOne, target: Vehicle, field: package }') | "entities[0].relations[0].field: 'package' is a Java keyword"
    entity('relations:\n      - { kind: manyToOne, target: Vehicle, column: user }') | "entities[0].relations[0].column: 'user' is a reserved word in SQL; give the relation a column"
    relations('A: [B]', 'B: [A]')                                                | "entities[0].relations[0].target: 'B' refers back to 'A'; a cycle of relations is not supported"
    relations('A: [B]', 'B: [C]', 'C: [A]')                                      | "entities[0].relations[0].target: 'B' refers back to 'A'; a cycle of relations is not supported"
    relations('A: [A, B]', 'B: [C]', 'C: [B]')                                   | "entities[1].relations[0].target: 'C' refers back to 'B'; a cycle of relations is not supported"
    relations('A: [B, B]', 'B: [C, A]', 'C: [A]')                                | "entities[0].relations[0].target: 'B' refers back to 'A'; a cycle of relations is not supported"
  }

  def "several problems are reported together, the structure before the meaning"() {
    when:
    SpecReader.parse('tib: 1\nentities:\n  - { name: vehicle, relations: [{ kind: manyToOne, target: Nope }] }')

    then:
    SpecException e = thrown()
    e.problems() == ['service: required', "entities[0].name: 'vehicle' does not match ${Names.TYPE}"]
    e.message == "the spec has 2 problems:\n  service: required\n  entities[0].name: 'vehicle' does not match ${Names.TYPE}"
  }

  def "relations that close no cycle are accepted"() {
    expect:
    SpecReader.parse(relations('A: [B, B, A]', 'B: []', 'C: [B, D]', 'D: [B]')).entities().size() == 4
  }

  def "every cycle of relations is reported once"() {
    when:
    SpecReader.parse(relations('A: [B, C]', 'B: [A]', 'C: [A]'))

    then:
    SpecException e = thrown()
    e.problems() == [
      "entities[0].relations[0].target: 'B' refers back to 'A'; a cycle of relations is not supported",
      "entities[0].relations[1].target: 'C' refers back to 'A'; a cycle of relations is not supported",
    ]
  }

  def "the list of the model is never null and never changes"() {
    expect:
    Spec.list(null) == []

    when:
    Spec.list(['a']).add('b')

    then:
    thrown(UnsupportedOperationException)
  }

  /** A spec of entities, each given as its name and the targets of its relations: {@code A: [B]}. */
  static String relations(String... entities) {
    'tib: 1\nservice: s\nentities:\n' + entities.collect { String entity ->
      String name = entity.substring(0, entity.indexOf(':'))
      List<String> targets = entity.substring(entity.indexOf('[') + 1, entity.indexOf(']')).split(',')*.trim().findAll()
      "  - name: $name\n    relations:\n" + targets.withIndex().collect { String target, int i ->
        "      - { kind: manyToOne, target: $target, field: to$i }\n"
      }.join('') + (targets ? '' : '      []\n')
    }.join('')
  }

  static String entity(String body) {
    "tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    $body"
  }

  static String field(String field) {
    entity("fields:\n      - $field")
  }
}
