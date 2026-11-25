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
package me.yakar.tib.pack.plain

import me.yakar.tib.generate.Project
import me.yakar.tib.pack.EntityModel
import me.yakar.tib.spec.SpecReader
import spock.lang.Specification

class DtoModelSpec extends Specification {

  def "the DTO has every value without annotations, and checks the rules of the spec: what is required and the length of a String"() {
    given:
    Map<String, EntityModel> models = EntityModel.modelsByIdentifier(new Project(SpecReader.parse('''\
      tib: 1
      service: s
      entities:
        - name: B
          id: { type: UUID }
        - name: A
          audit: true
          fields:
            - { name: code, type: String, length: 8, nullable: false }
            - { name: label, type: String }
            - { name: memo, type: Text }
            - { name: body, type: Text, nullable: false }
            - { name: count, type: Integer }
          relations:
            - { kind: manyToOne, target: B, nullable: false }
            - { kind: manyToOne, target: B, field: other }
      '''.stripIndent())))
    DtoModel dto = new DtoModel(models.A)

    expect:
    dto.components() == 'Long id, String code, String label, String memo, String body, Integer count, UUID bId, UUID otherId, Instant createdAt, Instant updatedAt'
    dto.checks() == [
      new DtoModel.Check('code', true, 8),
      new DtoModel.Check('label', false, 255),
      new DtoModel.Check('body', true, null),
      new DtoModel.Check('bId', true, null),
    ]
    dto.writableArguments('dto') == 'null, dto.code(), dto.label(), dto.memo(), dto.body(), dto.count(), dto.bId(), dto.otherId(), null, null'
    new DtoModel(models.B).components() == 'UUID id'
    new DtoModel(models.B).checks() == []
    new DtoModel(models.B).writableArguments('dto') == 'null'
  }
}
