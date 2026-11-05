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

import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import spock.lang.Specification

class EntityModelSpec extends Specification {

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

  def "a sample BigDecimal fits the precision and the scale of its column: #precision, #scale"() {
    given:
    FieldModel field = models("entities:\n  - name: A\n    fields: [{ name: amount, type: BigDecimal$size }]\n").A.fields()[0]

    expect:
    [field.sample(1), field.sample(2)] == samples.collect { 'new BigDecimal("' + it + '")' }

    where:
    precision | scale | size                          | samples
    null      | null  | ''                            | ['1', '2']
    12        | 2     | ', precision: 12, scale: 2'   | ['1.00', '2.00']
    null      | 2     | ', scale: 2'                  | ['1.00', '2.00']
    2         | 2     | ', precision: 2, scale: 2'    | ['0.01', '0.02']
  }

  /** The models of the entities of the spec, by name. */
  private static Map<String, EntityModel> models(String entities) {
    EntityGenerator.models(new Project(SpecReader.parse("tib: 1\nservice: s\n$entities")))
  }
}
