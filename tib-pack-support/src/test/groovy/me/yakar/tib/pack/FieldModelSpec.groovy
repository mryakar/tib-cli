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

class FieldModelSpec extends Specification {

  def "the value of a #type is a #javaType"() {
    expect:
    field("{ name: label, type: $type }").javaType() == javaType

    where:
    type         | javaType
    'String'     | 'String'
    'Text'       | 'String'
    'BigDecimal' | 'BigDecimal'
  }

  def "a sample BigDecimal fits the precision and the scale of its column: #precision, #scale"() {
    given:
    FieldModel field = field("{ name: amount, type: BigDecimal$size }")

    expect:
    [field.sample(1), field.sample(2)] == samples.collect { 'new BigDecimal("' + it + '")' }

    where:
    precision | scale | size                          | samples
    null      | null  | ''                            | ['1', '2']
    12        | 2     | ', precision: 12, scale: 2'   | ['1.00', '2.00']
    null      | 2     | ', scale: 2'                  | ['1.00', '2.00']
    2         | 2     | ', precision: 2, scale: 2'    | ['0.01', '0.02']
  }

  def "a sample String fits the length of its column, and a Text has none to fit: #type"() {
    given:
    FieldModel field = field("{ name: label, type: $type }")

    expect:
    [field.sample(1), field.sample(2)] == samples.collect { '"' + it + '"' }

    where:
    type                | samples
    'String'            | ['One', 'Two']
    'String, length: 2' | ['On', 'Tw']
    'String, length: 1' | ['O', 'T']
    'Text'              | ['One', 'Two']
  }

  def "a #type has its Java type, its import and two samples that differ"() {
    given:
    FieldModel field = field("{ name: v, type: $type }")

    expect:
    field.type().toString() == type
    [field.javaType(), field.typeImport(), field.text(), field.sample(1), field.sample(2)] == model

    where:
    type            | model
    'String'        | ['String', '', false, '"One"', '"Two"']
    'Text'          | ['String', '', true, '"One"', '"Two"']
    'Integer'       | ['Integer', '', false, '1', '2']
    'Long'          | ['Long', '', false, '1L', '2L']
    'BigDecimal'    | ['BigDecimal', 'import java.math.BigDecimal;', false, 'new BigDecimal("1")', 'new BigDecimal("2")']
    'Boolean'       | ['Boolean', '', false, 'true', 'false']
    'LocalDate'     | [
      'LocalDate',
      'import java.time.LocalDate;',
      false,
      'LocalDate.of(2026, 1, 1)',
      'LocalDate.of(2026, 1, 2)'
    ]
    'LocalDateTime' | [
      'LocalDateTime',
      'import java.time.LocalDateTime;',
      false,
      'LocalDateTime.of(2026, 1, 1, 12, 0)',
      'LocalDateTime.of(2026, 1, 2, 12, 0)'
    ]
    'Instant'       | [
      'Instant',
      'import java.time.Instant;',
      false,
      'Instant.parse("2026-01-01T12:00:00Z")',
      'Instant.parse("2026-01-02T12:00:00Z")'
    ]
    'UUID'          | [
      'UUID',
      'import java.util.UUID;',
      false,
      'UUID.fromString("00000000-0000-0000-0000-000000000001")',
      'UUID.fromString("00000000-0000-0000-0000-000000000002")'
    ]
  }

  def "the column and the validation of a field say what its spec says: #spec"() {
    given:
    FieldModel field = field(spec)

    expect:
    [field.column(), field.nullable(), field.unique(), field.length(), field.precision(), field.scale()] == column
    field.validation() == validation

    where:
    spec                                                                   | column                                  | validation
    '{ name: v, type: String }'                                            | ['V', true, false, 255, null, null]     | '@Size(max = 255) '
    '{ name: v, type: String, length: 20, nullable: false, unique: true }' | ['V', false, true, 20, null, null]      | '@NotNull @Size(max = 20) '
    '{ name: v, type: BigDecimal, precision: 12, scale: 2 }'               | ['V', true, false, null, 12, 2]         | ''
    '{ name: v, type: BigDecimal, precision: 12 }'                         | ['V', true, false, null, 12, null]      | ''
    '{ name: v, type: Integer, column: QTY, nullable: false }'             | ['QTY', false, false, null, null, null] | '@NotNull '
  }

  def "the accessors of a field take its name with a capital"() {
    given:
    FieldModel field = field('{ name: firstName, type: String }')

    expect:
    [field.name(), field.property()] == ['firstName', 'FirstName']
  }

  /** The model of the field of an entity of a spec. */
  private static FieldModel field(String field) {
    Models.of("entities:\n  - name: A\n    fields: [$field]\n").A.fields()[0]
  }
}
