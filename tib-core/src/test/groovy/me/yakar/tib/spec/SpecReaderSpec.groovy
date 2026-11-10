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

import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Specification

class SpecReaderSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  def "reads the example spec of the documentation and fills in the defaults"() {
    when:
    Spec spec = SpecReader.parse(Files.readString(EXAMPLES.resolve('vehicle-service.yaml')))

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
    vehicle.options() == new Options(true, true, true)
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
    depot.options() == new Options(true, true, true)
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
    Spec spec = SpecReader.parse(SpecSpec.entity('audit: false\n    options: { dto: false, mapper: false, pagination: false }\n'
        + '    relations:\n      - { kind: oneToOne, target: Vehicle, field: twin, column: TWIN_REF, nullable: false }'))

    then:
    !spec.entities()[0].audit()
    spec.entities()[0].options() == new Options(false, false, false)
    spec.entities()[0].relations() == [new Relation(RelationKind.ONE_TO_ONE, 'Vehicle', 'twin', 'TWIN_REF', false)]
  }

  def "a value that the reader cannot take is refused: #problem"() {
    when:
    SpecReader.parse(yaml)

    then:
    SpecException e = thrown()
    e.problems() == [problem]
    e.message == "the spec has a problem: $problem"

    where:
    yaml                                                    | problem
    SpecSpec.field('{ name: x, type: Strng }')              | "entities[0].fields[0].type: 'Strng' is not one of String, Integer, Long, BigDecimal, Boolean, LocalDate, LocalDateTime, Instant, UUID"
    SpecSpec.field('{ name: x, type: String, lenght: 3 }')  | "entities[0].fields[0].lenght: unknown property; the properties are column, length, name, nullable, precision, scale, type, unique"
    SpecSpec.field('{ name: x, type: String, length: abc }') | "entities[0].fields[0].length: 'abc' is not of type Integer"
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
}
