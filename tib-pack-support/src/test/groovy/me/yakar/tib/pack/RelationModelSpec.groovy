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

class RelationModelSpec extends Specification {

  def "a relation is held by its field and its column, by its kind: #spec"() {
    given:
    RelationModel relation = Models.of("""\
      entities:
        - name: B
        - name: C
          id: { type: UUID, field: code }
        - name: A
          relations: [$spec]
      """.stripIndent()).A.relations()[0]

    expect:
    [relation.field(), relation.property(), relation.idField(), relation.idProperty(), relation.column()] == names
    [relation.annotation(), relation.mapping(), relation.joinColumn(), relation.idColumn()] == annotations
    [relation.validation(), relation.sql()] == dto

    where:
    spec                                                       | names                                      | annotations                                                                                                                                                                          | dto
    '{ kind: manyToOne, target: B }'                           | ['b', 'B', 'bId', 'BId', 'B_ID']           | [
      'ManyToOne',
      '@ManyToOne(fetch = FetchType.LAZY)',
      '@JoinColumn(name = "B_ID")',
      '@Column(name = "B_ID")'
    ]                                                                         | ['', 'B_ID BIGINT REFERENCES B (ID)']
    '{ kind: oneToOne, target: B, field: other, nullable: false }' | ['other', 'Other', 'otherId', 'OtherId', 'OTHER_ID'] | [
      'OneToOne',
      '@OneToOne(fetch = FetchType.LAZY, optional = false)',
      '@JoinColumn(name = "OTHER_ID", nullable = false, unique = true)',
      '@Column(name = "OTHER_ID", nullable = false, unique = true)'
    ] | ['@NotNull ', 'OTHER_ID BIGINT NOT NULL UNIQUE REFERENCES B (ID)']
    '{ kind: manyToOne, target: C }'                           | ['c', 'C', 'cCode', 'CCode', 'C_ID']       | [
      'ManyToOne',
      '@ManyToOne(fetch = FetchType.LAZY)',
      '@JoinColumn(name = "C_ID")',
      '@Column(name = "C_ID")'
    ]                                                                         | ['', 'C_ID UUID REFERENCES C (CODE)']
  }
}
