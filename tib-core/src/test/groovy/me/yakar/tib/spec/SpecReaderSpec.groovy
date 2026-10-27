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
    vehicle.relations() == [new Relation(RelationKind.MANY_TO_ONE, 'Depot', 'depot', 'DEPOT_ID')]

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
    Spec spec = SpecReader.read('tib: 1\nservice: s\n')

    then:
    spec == new Spec(1, 's', 'default', [])
  }

  def "what the spec says explicitly is kept"() {
    when:
    Spec spec = SpecReader.read(entity('audit: false\n    options: { dto: false, mapper: false, pagination: false, events: false }\n'
        + '    relations:\n      - { kind: oneToOne, target: Vehicle, field: twin, column: TWIN_REF }'))

    then:
    !spec.entities()[0].audit()
    spec.entities()[0].options() == new Options(false, false, false, false)
    spec.entities()[0].relations() == [new Relation(RelationKind.ONE_TO_ONE, 'Vehicle', 'twin', 'TWIN_REF')]
  }

  def "a spec with a problem is refused: #problem"() {
    when:
    SpecReader.read(yaml)

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
  }

  def "several problems are reported together, the structure before the meaning"() {
    when:
    SpecReader.read('tib: 2\nentities:\n  - { name: vehicle }')

    then:
    SpecException e = thrown()
    e.problems() == ['service: required', "entities[0].name: 'vehicle' does not match ${Names.TYPE}"]
    e.message == "the spec has 2 problems:\n  service: required\n  entities[0].name: 'vehicle' does not match ${Names.TYPE}"
  }

  def "a value of the wrong shape is refused with the reader's own words"() {
    when:
    SpecReader.read('tib: [1]\nservice: s')

    then:
    SpecException e = thrown()
    e.problems().size() == 1
    e.problems()[0].startsWith('tib: ')
  }

  def "an empty file is refused"() {
    when:
    SpecReader.read('')

    then:
    SpecException e = thrown()
    e.problems()[0].startsWith('spec: ')
  }

  def "a file that is not YAML is refused"() {
    when:
    SpecReader.read("tib: 1\nservice: 'unterminated")

    then:
    SpecException e = thrown()
    e.problems()[0].startsWith('not YAML: ')
  }

  def "a file that is not there cannot be read"() {
    when:
    SpecReader.read(EXAMPLES.resolve('no-such-service.yaml'))

    then:
    thrown(UncheckedIOException)
  }

  private static String entity(String body) {
    "tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    $body"
  }

  private static String field(String field) {
    entity("fields:\n      - $field")
  }
}
