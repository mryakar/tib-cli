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

import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Shared
import spock.lang.Specification

class SpecSchemaSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  static final Path SNAPSHOTS = Path.of(System.getProperty('tib.snapshots'))

  @Shared
  Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(SpecSchema.json())

  def "the schema changes only on purpose"() {
    given:
    String snapshot = Files.readString(SNAPSHOTS.resolve('tib-spec.schema.json'))
    Path actual = Path.of('build', 'snapshots', 'tib-spec.schema.json')
    Files.createDirectories(actual.parent)
    Files.writeString(actual, SpecSchema.json())

    expect: "if not, the new schema is in $actual, to be copied over the snapshot on purpose"
    SpecSchema.json() == snapshot
  }

  def "the example spec of the documentation is valid"() {
    expect:
    schema.validate(Files.readString(EXAMPLES.resolve('vehicle-service.yaml')), InputFormat.YAML).empty
  }

  def "the schema refuses a spec of the wrong structure: #yaml"() {
    expect:
    !schema.validate(yaml, InputFormat.YAML).empty

    where:
    yaml << [
      'service: s',
      'tib: 2\nservice: s',
      'tib: 0\nservice: s',
      'tib: 1\nservice: Vehicle Service',
      'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    fields:\n      - { name: x, type: Strng }',
      'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    fields:\n      - { name: x, type: String, lenght: 3 }',
      'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    fields:\n      - { name: x, type: String, length: 0 }',
      'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    fields:\n      - { name: x }',
      'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    relations:\n      - { kind: manyToMany, target: Depot }',
      'tib: 1\nservice: s\nentities:\n  - name: Vehicle\n    relations:\n      - { kind: manyToOne, target: Depot, nullable: maybe }',
    ]
  }

  def "a record without constraints gets a schema without them"() {
    expect:
    SpecSchema.of(Plain) == [
      type: 'object',
      description: 'a plain record',
      properties: [
        number: [description: 'a number', type: 'integer'],
        text: [description: 'a text', type: 'string'],
      ],
      additionalProperties: false,
    ]
  }

  def "the default of #record.simpleName.#component.name in the schema is the one the record takes"() {
    given:
    Object resolved = record.getDeclaredConstructor(record.recordComponents*.type as Class[])
        .newInstance(new Object[record.recordComponents.length])

    expect:
    String.valueOf(component.accessor.invoke(resolved)) == component.getAnnotation(Doc).defaultValue()

    where:
    [record, component] << [Spec, Entity, Id, Field, Relation, Options].collectMany { type ->
      type.recordComponents.findAll {
        !it.getAnnotation(Doc).defaultValue().empty
      }.collect {
        [type, it]
      }
    }
  }
}
