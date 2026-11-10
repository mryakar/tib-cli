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

class EntityTestsModelSpec extends Specification {

  def "the entities that an entity needs come once each, each after the ones it needs, without itself"() {
    given:
    Map<String, EntityTestsModel> tests = tests('''\
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
    tests.A.requiredTargets()*.name() == ['C', 'B']
  }

  def "two entities with the same values break a constraint when #what"() {
    expect:
    tests("entities:\n  - name: B\n  - name: A\n    $members\n").A.unique() == unique

    where:
    what                                   | members                                                                    | unique
    'a field is unique'                    | 'fields: [{ name: code, type: String, unique: true }]'                     | true
    'a one-to-one relation is not nullable' | 'relations: [{ kind: oneToOne, target: B, nullable: false }]'             | true
    'a one-to-one relation is nullable'    | 'relations: [{ kind: oneToOne, target: B }]'                               | false
    'a many-to-one relation is not nullable' | 'relations: [{ kind: manyToOne, target: B, nullable: false }]'           | false
  }

  /** The models of the tests of the entities of the spec, by name. */
  private static Map<String, EntityTestsModel> tests(String entities) {
    EntityGenerator.models(new Project(SpecReader.parse("tib: 1\nservice: s\n$entities"))).collectEntries { name, model ->
      [name, new EntityTestsModel(model)]
    }
  }
}
