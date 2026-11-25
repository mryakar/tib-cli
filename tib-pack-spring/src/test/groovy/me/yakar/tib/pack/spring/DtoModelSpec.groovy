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
package me.yakar.tib.pack.spring

import me.yakar.tib.generate.Project
import me.yakar.tib.pack.EntityModel
import me.yakar.tib.spec.SpecReader
import spock.lang.Specification

class DtoModelSpec extends Specification {

  def "the DTO has every value in the order of the spec, each after the validation of its rules"() {
    given:
    Map<String, EntityModel> models = EntityModel.models(new Project(SpecReader.parse('''\
      tib: 1
      service: s
      entities:
        - name: B
        - name: C
          id: { type: UUID }
        - name: A
          audit: true
          fields:
            - { name: label, type: String, nullable: false }
            - { name: code, type: String, length: 20 }
            - { name: count, type: Integer, nullable: false }
            - { name: memo, type: Text }
          relations:
            - { kind: manyToOne, target: B }
            - { kind: oneToOne, target: C, nullable: false }
            - { kind: manyToOne, target: A, field: parent }
      '''.stripIndent())))

    expect:
    new DtoModel(models.A).components() == 'Long id, @NotNull @Size(max = 255) String label, @Size(max = 20) String code, @NotNull Integer count, String memo, Long bId, @NotNull UUID cId, Long parentId, Instant createdAt, Instant updatedAt'
    new DtoModel(models.B).components() == 'Long id'
  }
}
