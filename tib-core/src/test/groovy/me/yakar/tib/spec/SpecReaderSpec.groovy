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

import java.nio.file.NoSuchFileException
import java.nio.file.Path
import spock.lang.Specification

class SpecReaderSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  def "reads the example spec of the documentation and fills in the defaults"() {
    when:
    Spec spec = SpecReader.read(EXAMPLES.resolve('vehicle-service.yaml'))

    then:
    spec.tib() == 1
    spec.service() == 'vehicle-service'
    spec.group() == 'com.example'
    spec.pack() == 'default'
    spec.entities()*.name() == ['Vehicle', 'Depot']

    and: 'the vehicle takes the defaults'
    Entity vehicle = spec.entities()[0]
    vehicle.table() == 'VEHICLE'
    vehicle.id() == new Id('id', IdType.LONG, IdStrategy.IDENTITY)
    vehicle.audit()
    vehicle.options() == new Options(true, true, true, false)
    vehicle.fields()[0] == new Field('plateNumber', 'PLATE_NUMBER', FieldType.STRING, 16, null, null, false, true)
    vehicle.fields()[1] == new Field('capacity', 'CAPACITY', FieldType.INTEGER, null, null, null, true, false)
    vehicle.fields()[2].precision() == 12
    vehicle.fields()[2].scale() == 2
    vehicle.relations() == [new Relation(RelationKind.MANY_TO_ONE, 'Depot', 'depot', 'DEPOT_ID', true)]

    and: 'the depot keeps what the spec says'
    Entity depot = spec.entities()[1]
    depot.table() == 'DEPOTS'
    depot.id() == new Id('id', IdType.UUID, IdStrategy.UUID)
    depot.fields()[0].column() == 'DEPOT_CODE'
    depot.fields()[1].type() == FieldType.LOCAL_DATE
    !depot.audit()
    depot.options() == new Options(true, true, true, true)
    depot.relations().empty
  }

  def "a spec with only the required values is complete"() {
    when:
    Spec spec = SpecReader.parse('tib: 1\nservice: s\n')

    then:
    spec == new Spec(1, 's', 'com.example', 'default', [])
  }

  def "what the spec says explicitly is kept"() {
    when:
    Spec spec = SpecReader.parse(entity('audit: false\n    options: { dto: false, mapper: false, pagination: false, events: false }\n'
        + '    relations:\n      - { kind: oneToOne, target: Vehicle, field: twin, column: TWIN_REF, nullable: false }'))

    then:
    !spec.entities()[0].audit()
    spec.entities()[0].options() == new Options(false, false, false, false)
    spec.entities()[0].relations() == [new Relation(RelationKind.ONE_TO_ONE, 'Vehicle', 'twin', 'TWIN_REF', false)]
  }

  def "a spec with a problem is refused: #problem"() {
    when:
    SpecReader.parse(yaml)

    then:
    SpecException e = thrown()
    e.problems() == [problem]
    e.message == "the spec has a problem: $problem"

    where:
    yaml                                                                        | problem
    field('{ name: x, type: Strng }')                                           | "entities[0].fields[0].type: 'Strng' is not one of String, Integer, Long, BigDecimal, Boolean, LocalDate, LocalDateTime, Instant, UUID"
    field('{ name: x, type: String, lenght: 3 }')                               | "entities[0].fields[0].lenght: unknown property; the properties are column, length, name, nullable, precision, scale, type, unique"
    field('{ name: x, type: String, length: abc }')                             | "entities[0].fields[0].length: 'abc' is not of type Integer"
    'service: s'                                                                | 'tib: required'
    'tib: 0\nservice: s'                                                        | 'tib: 0 is below 1'
    'tib: 2\nservice: s'                                                        | 'tib: this build reads version 1'
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
    entity('audit: true\n    fields:\n      - { name: createdAt, type: Instant }') | "entities[0].fields[0].column: 'CREATED_AT' is already a column"
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
  }

  def "several problems are reported together, the structure before the meaning"() {
    when:
    SpecReader.parse('tib: 2\nentities:\n  - { name: vehicle }')

    then:
    SpecException e = thrown()
    e.problems() == ['service: required', "entities[0].name: 'vehicle' does not match ${Names.TYPE}"]
    e.message == "the spec has 2 problems:\n  service: required\n  entities[0].name: 'vehicle' does not match ${Names.TYPE}"
  }

  def "a value of the wrong shape is refused with the reader's own words: #yaml"() {
    when:
    SpecReader.parse(yaml)

    then:
    SpecException e = thrown()
    e.problems().size() == 1
    e.problems()[0].startsWith(path)

    where:
    yaml                  | path
    'tib: [1]\nservice: s' | 'tib: '
    '- tib: 1'            | 'spec: '
  }

  def "an empty file is refused: '#yaml'"() {
    when:
    SpecReader.parse(yaml)

    then:
    SpecException e = thrown()
    e.message == 'the spec has a problem: spec: the file is empty'

    where:
    yaml << ['', '---\n', '# nothing\n']
  }

  def "a file that is not YAML is refused"() {
    when:
    SpecReader.parse("tib: 1\nservice: 'unterminated")

    then:
    SpecException e = thrown()
    e.problems()[0].startsWith('not YAML: ')
  }

  def "a file that is not there cannot be read"() {
    given:
    Path file = EXAMPLES.resolve('no-such-service.yaml')

    when:
    SpecReader.read(file)

    then:
    SpecException e = thrown()
    e.message == "cannot read $file: no such file"
    e.problems() == [e.message]
    e.cause instanceof NoSuchFileException
  }

  private static String entity(String body) {
    "tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    $body"
  }

  private static String field(String field) {
    entity("fields:\n      - $field")
  }
}
