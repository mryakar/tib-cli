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

  /** A spec with an entity that needs another, which needs a third, and has relations that it does not need. */
  static final String NEEDS = '''\
    entities:
      - name: C
        id: { type: UUID }
      - name: B
        relations:
          - { kind: manyToOne, target: C, nullable: false }
      - name: A
        audit: true
        fields:
          - { name: label, type: String, nullable: false }
          - { name: code, type: Integer }
        relations:
          - { kind: manyToOne, target: B, nullable: false }
          - { kind: oneToOne, target: C }
          - { kind: manyToOne, target: A, field: parent }
    '''.stripIndent()

  def "the samples of an entity take the samples of what it needs: #architecture"() {
    given:
    Map<String, EntityTestsModel> tests = tests("architecture: $architecture\n$NEEDS")
    EntityTestsModel a = tests.A

    expect:
    a.entity().name() == 'A'
    a.pluralType() == 'As'
    a.requiredParameters() == parameters
    a.requiredIdParameters() == 'Long bId'
    a.sample(a.entity()) == sample
    tests.C.sample(tests.C.entity()) == 'CSamples.c().build()'
    a.requiredIdArguments() == 'b.getId()'
    a.sampleIdArguments() == '1L'
    tests.B.sampleIdArguments() == 'UUID.fromString("00000000-0000-0000-0000-000000000001")'
    a.sampleValues(1) == values
    a.dtoSampleValues(2) == '.label("Two").code(2).bId(bId)'
    a.builderProperties() == properties.collect { new EntityTestsModel.Property(*it) }

    where:
    architecture | parameters | sample                        | values                         | properties
    'anemic'     | 'B b'      | 'ASamples.a(b).build()'       | '.label("One").code(1).b(b)'   | [['String', 'label'], ['Integer', 'code'], ['B', 'b'], ['C', 'c'], ['A', 'parent']]
    'ddd'        | 'Long bId' | 'ASamples.a(b.getId()).build()' | '.label("One").code(1).bId(bId)' | [['String', 'label'], ['Integer', 'code'], ['Long', 'bId'], ['UUID', 'cId'], ['Long', 'parentId']]
  }

  def "the tests save an entity and delete its rows by the architecture: #architecture"() {
    given:
    Map<String, EntityTestsModel> tests = tests("architecture: $architecture\n$NEEDS")

    expect:
    tests.A.save() == save
    tests.A.repositoryVariable(tests.B.entity()) == repository

    where:
    architecture | save           | repository
    'anemic'     | 'saveAndFlush' | 'bs'
    'hexagonal'  | 'save'         | 'bRepository'
  }

  def "the test of the persistence adapter imports the adapters and the mappers of the entity and of what it needs"() {
    expect:
    tests("architecture: hexagonal\n$NEEDS").A.persistenceClasses() ==
        'TestcontainersConfiguration.class, APersistenceAdapter.class, APersistenceMapper.class, CPersistenceAdapter.class, CPersistenceMapper.class, BPersistenceAdapter.class, BPersistenceMapper.class'
  }

  def "the DTO of the samples has every value, and the identifiers of the tests fit the type of the identifier"() {
    given:
    Map<String, EntityTestsModel> tests = tests(NEEDS)

    expect:
    tests.A.dtoBuilderProperties() == [
      ['Long', 'id'],
      ['String', 'label'],
      ['Integer', 'code'],
      ['Long', 'bId'],
      ['UUID', 'cId'],
      ['Long', 'parentId']
    ].collect {
      new EntityTestsModel.Property(*it)
    }
    tests.A.dtoBuilderArguments() == 'id, label, code, bId, cId, parentId, null, null'
    tests.C.dtoBuilderArguments() == 'id'
    tests.A.ignoredFields() == '"id", "createdAt", "updatedAt"'
    tests.C.ignoredFields() == '"id"'
    [tests.A.sampleId(), tests.A.unknownId()] == ['1L', 'Long.MAX_VALUE']
    [tests.C.sampleId(), tests.C.unknownId()] == ['UUID.fromString("00000000-0000-0000-0000-000000000001")', 'UUID.randomUUID()']
  }

  def "a DTO without values breaks a constraint when #what"() {
    expect:
    tests("entities:\n  - name: B\n  - name: A\n    $members\n").A.requiresValues() == requires

    where:
    what                                   | members                                                        | requires
    'a field is not nullable'              | 'fields: [{ name: note, type: Text, nullable: false }]'       | true
    'every field is nullable'              | 'fields: [{ name: note, type: Text }]'                         | false
    'a relation is not nullable'           | 'relations: [{ kind: manyToOne, target: B, nullable: false }]' | true
    'every relation is nullable'           | 'relations: [{ kind: manyToOne, target: B }]'                  | false
  }

  /** The models of the tests of the entities of the spec, by name. */
  private static Map<String, EntityTestsModel> tests(String entities) {
    Models.of(entities).collectEntries { name, model ->
      [name, new EntityTestsModel(model)]
    }
  }
}
