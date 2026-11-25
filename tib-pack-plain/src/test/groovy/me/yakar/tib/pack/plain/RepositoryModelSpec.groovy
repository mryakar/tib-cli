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

class RepositoryModelSpec extends Specification {

  def "the statements bind the values of the entity, and read back what the database gives the row: #what"() {
    given:
    RepositoryModel repository = new RepositoryModel(models(entities).A)

    expect:
    repository.columns() == columns
    repository.insert() == insert
    repository.insertParameters() == insertParameters
    repository.insertReads()*.property() == insertReads
    repository.update() == update
    repository.updateParameters() == updateParameters
    repository.updateReads()*.property() == (audited ? ['UpdatedAt'] : [])

    where:
    what | entities | columns | insert | insertParameters | insertReads | update | updateParameters | audited
    'an identity without values' | '  - name: A\n' | 'ID' | 'INSERT INTO A DEFAULT VALUES RETURNING ID' | [] | ['Id'] | 'UPDATE A SET ID = ID WHERE ID = ?' | ['a.getId()'] | false
    'a sequence without values' | '  - name: A\n    id: { strategy: sequence }\n' | 'ID' | "INSERT INTO A (ID) VALUES (nextval('A_SEQ')) RETURNING ID" | [] | ['Id'] | 'UPDATE A SET ID = ID WHERE ID = ?' | ['a.getId()'] | false
    'a UUID without values' | '  - name: A\n    id: { type: UUID }\n' | 'ID' | 'INSERT INTO A (ID) VALUES (?)' | ['a.getId()'] | [] | 'UPDATE A SET ID = ID WHERE ID = ?' | ['a.getId()'] | false
    'a UUID, audited, with a field and a relation' | '  - name: B\n  - name: A\n    id: { type: UUID, column: KEY }\n    audit: true\n    fields: [{ name: seenAt, type: Instant }]\n    relations: [{ kind: manyToOne, target: B }]\n' | 'KEY, SEEN_AT, B_ID, CREATED_AT, UPDATED_AT' | 'INSERT INTO A (KEY, SEEN_AT, B_ID, CREATED_AT, UPDATED_AT) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING CREATED_AT, UPDATED_AT' | ['a.getId()', 'offset(a.getSeenAt())', 'a.getBId()'] | ['CreatedAt', 'UpdatedAt'] | 'UPDATE A SET SEEN_AT = ?, B_ID = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE KEY = ? RETURNING UPDATED_AT' | ['offset(a.getSeenAt())', 'a.getBId()', 'a.getId()'] | true
    'an identity with a field, audited in lower case' | '  - name: A\n    table: a\n    audit: true\n    fields: [{ name: label, type: String }]\n' | 'ID, LABEL, created_at, updated_at' | 'INSERT INTO a (LABEL, created_at, updated_at) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING ID, created_at, updated_at' | ['a.getLabel()'] | ['Id', 'CreatedAt', 'UpdatedAt'] | 'UPDATE a SET LABEL = ?, updated_at = CURRENT_TIMESTAMP WHERE ID = ? RETURNING updated_at' | ['a.getLabel()', 'a.getId()'] | true
  }

  def "a row is read into every value of the entity, an Instant through the time of the driver"() {
    given:
    RepositoryModel repository = new RepositoryModel(models('''\
      - name: B
        id: { type: UUID }
      - name: A
        audit: true
        fields:
          - { name: label, type: Text }
          - { name: seenAt, type: Instant }
        relations:
          - { kind: manyToOne, target: B }
      '''.stripIndent()).A)

    expect:
    repository.instants()
    repository.reads().collect { [it.property(), it.value()] } == [
      ['Id', 'rows.getObject("ID", Long.class)'],
      ['Label', 'rows.getObject("LABEL", String.class)'],
      ['SeenAt', 'instant(rows.getObject("SEEN_AT", OffsetDateTime.class))'],
      ['BId', 'rows.getObject("B_ID", UUID.class)'],
      ['CreatedAt', 'rows.getObject("CREATED_AT", OffsetDateTime.class).toInstant()'],
      ['UpdatedAt', 'rows.getObject("UPDATED_AT", OffsetDateTime.class).toInstant()'],
    ]
    !new RepositoryModel(models('  - name: A\n').A).instants()
  }

  /** The models of the entities, as the plain pack makes them: each refers to another by its identifier. */
  private static Map<String, EntityModel> models(String entities) {
    EntityModel.modelsByIdentifier(new Project(SpecReader.parse("tib: 1\nservice: s\nentities:\n$entities")))
  }
}
