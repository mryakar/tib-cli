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
import me.yakar.tib.spec.Entity
import me.yakar.tib.spec.SpecReader
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

  def "the entities that an entity needs come once each, each after the ones it needs, without itself"() {
    given:
    Map<String, EntityModel> models = models('''\
      entities:
        - name: C
        - name: B
          relations:
            - { kind: manyToOne, target: C, nullable: false }
        - name: D
        - name: A
          relations:
            - { kind: manyToOne, target: B, nullable: false }
            - { kind: manyToOne, target: B, field: other, nullable: false }
            - { kind: manyToOne, target: A, field: parent, nullable: false }
            - { kind: manyToOne, target: D }
      '''.stripIndent())

    expect:
    models.A.requiredTargets()*.name() == ['C', 'B']
    models.A.reversedRequiredTargets()*.name() == ['B', 'C']
    models.A.sampleSetters() == ['a.setB(b);', 'a.setOther(b);']
  }

  def "two entities with the same values break a constraint when #what"() {
    expect:
    models("entities:\n  - name: B\n  - name: A\n    $members\n").A.unique() == unique

    where:
    what                                   | members                                                                    | unique
    'a field is unique'                    | 'fields: [{ name: code, type: String, unique: true }]'                     | true
    'a one-to-one relation is not nullable' | 'relations: [{ kind: oneToOne, target: B, nullable: false }]'             | true
    'a one-to-one relation is nullable'    | 'relations: [{ kind: oneToOne, target: B }]'                               | false
    'a many-to-one relation is not nullable' | 'relations: [{ kind: manyToOne, target: B, nullable: false }]'           | false
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

  /** The models of the entities of the spec, by name, built as the generator builds them. */
  private static Map<String, EntityModel> models(String entities) {
    Project project = new Project(SpecReader.parse("tib: 1\nservice: s\n$entities"))
    Map<String, EntityModel> models = [:]
    for (Entity entity : EntityGenerator.ordered(project.spec().entities())) {
      models[entity.name()] = new EntityModel(project, entity, models)
    }
    models
  }
}
