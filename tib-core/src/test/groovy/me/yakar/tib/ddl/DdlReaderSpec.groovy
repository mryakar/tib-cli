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
package me.yakar.tib.ddl

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.spec.Entity
import me.yakar.tib.spec.Field
import me.yakar.tib.spec.FieldType
import me.yakar.tib.spec.Id
import me.yakar.tib.spec.IdStrategy
import me.yakar.tib.spec.IdType
import me.yakar.tib.spec.Options
import me.yakar.tib.spec.Relation
import me.yakar.tib.spec.RelationKind
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecException
import me.yakar.tib.spec.SpecReader
import spock.lang.Specification
import spock.lang.TempDir

class DdlReaderSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  static final Spec SERVICE = SpecReader.read('tib: 1\nservice: vehicle-service\ngroup: me.yakar\n')

  @TempDir
  Path dir

  def "reads the tables of the example DDL into the spec of the service"() {
    when:
    Spec spec = DdlReader.read(EXAMPLES.resolve('vehicle-service.sql'), SERVICE)

    then:
    spec.service() == 'vehicle-service'
    spec.group() == 'me.yakar'
    spec.entities() == [
      new Entity('Depot', 'depots', new Id('id', IdType.UUID, IdStrategy.UUID), [
        new Field('code', 'code', FieldType.STRING, 8, null, null, false, true),
        new Field('openedOn', 'opened_on', FieldType.LOCAL_DATE, null, null, null, true, false),
      ], [], false, null),
      new Entity('Vehicle', 'vehicles', new Id('id', IdType.LONG, IdStrategy.IDENTITY), [
        new Field('plateNumber', 'plate_number', FieldType.STRING, 16, null, null, false, true),
        new Field('capacity', 'capacity', FieldType.INTEGER, null, null, null, true, false),
        new Field('price', 'price', FieldType.BIG_DECIMAL, null, 12, 2, true, false),
      ], [new Relation(RelationKind.MANY_TO_ONE, 'Depot', 'depot', 'depot_id', true)], true, null),
    ]
  }

  def "a file that is not there is an I/O problem"() {
    when:
    DdlReader.read(dir.resolve('missing.sql'), SERVICE)

    then:
    thrown(UncheckedIOException)
  }

  def "the SQL type #sql is a #type"() {
    when:
    Field field = entity("CREATE TABLE T (ID BIGINT PRIMARY KEY, F $sql);").fields()[0]

    then:
    field.type() == type
    field.length() == length
    field.precision() == precision
    field.scale() == scale

    where:
    sql                              | type                      | length | precision | scale
    'VARCHAR(40)'                    | FieldType.STRING          | 40     | null      | null
    'varchar2(20 char)'              | FieldType.STRING          | 20     | null      | null
    'CHARACTER VARYING(5)'           | FieldType.STRING          | 5      | null      | null
    'NVARCHAR2(3)'                   | FieldType.STRING          | 3      | null      | null
    'NVARCHAR(3)'                    | FieldType.STRING          | 3      | null      | null
    'CHAR(2)'                        | FieldType.STRING          | 2      | null      | null
    'CHARACTER(2)'                   | FieldType.STRING          | 2      | null      | null
    'NCHAR(2)'                       | FieldType.STRING          | 2      | null      | null
    'TEXT'                           | FieldType.STRING          | null   | null      | null
    'CLOB'                           | FieldType.STRING          | null   | null      | null
    'NCLOB'                          | FieldType.STRING          | null   | null      | null
    'SMALLINT'                       | FieldType.INTEGER         | null   | null      | null
    'INT'                            | FieldType.INTEGER         | null   | null      | null
    'INTEGER'                        | FieldType.INTEGER         | null   | null      | null
    'INT2'                           | FieldType.INTEGER         | null   | null      | null
    'INT4'                           | FieldType.INTEGER         | null   | null      | null
    'SERIAL'                         | FieldType.INTEGER         | null   | null      | null
    'SMALLSERIAL'                    | FieldType.INTEGER         | null   | null      | null
    'BIGINT'                         | FieldType.LONG            | null   | null      | null
    'INT8'                           | FieldType.LONG            | null   | null      | null
    'BIGSERIAL'                      | FieldType.LONG            | null   | null      | null
    'NUMBER(9)'                      | FieldType.INTEGER         | null   | null      | null
    'NUMBER(10, 0)'                  | FieldType.LONG            | null   | null      | null
    'NUMBER(19)'                     | FieldType.LONG            | null   | null      | null
    'NUMBER(20)'                     | FieldType.BIG_DECIMAL     | null   | 20        | 0
    'NUMERIC(12, 2)'                 | FieldType.BIG_DECIMAL     | null   | 12        | 2
    'DECIMAL(5,1)'                   | FieldType.BIG_DECIMAL     | null   | 5         | 1
    'NUMERIC'                        | FieldType.BIG_DECIMAL     | null   | null      | null
    'BOOLEAN'                        | FieldType.BOOLEAN         | null   | null      | null
    'BOOL'                           | FieldType.BOOLEAN         | null   | null      | null
    'DATE'                           | FieldType.LOCAL_DATE      | null   | null      | null
    'TIMESTAMP'                      | FieldType.LOCAL_DATE_TIME | null   | null      | null
    'TIMESTAMP(6) WITHOUT TIME ZONE' | FieldType.LOCAL_DATE_TIME | null   | null      | null
    'DATETIME'                       | FieldType.LOCAL_DATE_TIME | null   | null      | null
    'TIMESTAMPTZ'                    | FieldType.INSTANT         | null   | null      | null
    'TIMESTAMP WITH TIME ZONE'       | FieldType.INSTANT         | null   | null      | null
    'UUID'                           | FieldType.UUID            | null   | null      | null
  }

  def "the SQL type #sql is not supported"() {
    when:
    entity("CREATE TABLE T (ID BIGINT PRIMARY KEY, F $sql);")

    then:
    DdlException e = thrown()
    e.problems() == ["T.F: the type $text is not supported"]
    e.message == "the DDL has a problem: T.F: the type $text is not supported"

    where:
    sql                | text
    'MONEY'            | 'MONEY'
    'DOUBLE PRECISION' | 'DOUBLE PRECISION'
    'INTEGER[]'        | 'INTEGER'
    'VARCHAR(MAX)'     | 'VARCHAR(MAX)'
  }

  def "the primary key #sql is an identifier of #type by #strategy"() {
    expect:
    entity("CREATE TABLE T (ID $sql);").id() == new Id('id', type, strategy)

    where:
    sql                                                | type        | strategy
    'BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY' | IdType.LONG | IdStrategy.IDENTITY
    'BIGINT AUTO_INCREMENT PRIMARY KEY'                | IdType.LONG | IdStrategy.IDENTITY
    'BIGSERIAL PRIMARY KEY'                            | IdType.LONG | IdStrategy.IDENTITY
    'SERIAL PRIMARY KEY'                               | IdType.LONG | IdStrategy.IDENTITY
    'NUMBER(19) PRIMARY KEY'                           | IdType.LONG | IdStrategy.SEQUENCE
    'INT PRIMARY KEY'                                  | IdType.LONG | IdStrategy.SEQUENCE
    'UUID PRIMARY KEY'                                 | IdType.UUID | IdStrategy.UUID
  }

  def "the identifier is named after its column"() {
    expect:
    entity('CREATE TABLE T (T_ID BIGINT, CONSTRAINT T_PK PRIMARY KEY (T_ID));').id().field() == 'tId'
  }

  def "a primary key of #sql is a problem"() {
    when:
    entity("CREATE TABLE T (ID $sql PRIMARY KEY);")

    then:
    DdlException e = thrown()
    e.problems() == ["T.ID: the primary key is $sql, and only an integer or a UUID is supported"]

    where:
    sql << ['VARCHAR(10)', 'MONEY']
  }

  def "a table without a primary key, or with one of several columns, is a problem"() {
    when:
    DdlReader.read('CREATE TABLE A (X INT);\nCREATE TABLE B (X INT, Y INT, PRIMARY KEY (X, Y));', SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == [
      'A: there is no primary key',
      'B: a primary key of several columns is not supported'
    ]
    e.message == '''the DDL has 2 problems:
  A: there is no primary key
  B: a primary key of several columns is not supported'''
  }

  def "a column is null or unique as its definition and the keys of the table say"() {
    when:
    List<Field> fields = entity('''
      CREATE TABLE T (
        ID BIGINT PRIMARY KEY,
        A INT NOT NULL,
        B INT CONSTRAINT T_B UNIQUE,
        C INT,
        D INT,
        E INT,
        F INT,
        CONSTRAINT T_C UNIQUE (C),
        UNIQUE KEY T_D (D),
        UNIQUE (E, F),
        INDEX T_E (E),
        CONSTRAINT T_CHECK CHECK (E > 0)
      );
      ALTER TABLE T ADD CONSTRAINT T_F UNIQUE (F);
      ''').fields()

    then:
    fields.collect { [it.name(), it.nullable(), it.unique()] } == [
      ['a', false, false],
      ['b', true, true],
      ['c', true, true],
      ['d', true, true],
      ['e', true, false],
      ['f', true, true],
    ]
  }

  def "a column that references a table is a relation to its entity"() {
    when:
    Entity entity = DdlReader.read('''
      CREATE TABLE DEPOT (ID BIGINT PRIMARY KEY);
      CREATE TABLE "Vehicle_Types" (ID BIGINT PRIMARY KEY);
      CREATE TABLE VEHICLE (
        ID BIGINT PRIMARY KEY,
        DEPOT_ID BIGINT NOT NULL REFERENCES DEPOT (ID),
        HOME_DEPOT_ID BIGINT UNIQUE REFERENCES public.depot,
        TYPE_ID BIGINT,
        OWNER BIGINT,
        PREVIOUS_ID BIGINT,
        FOREIGN KEY (TYPE_ID) REFERENCES "Vehicle_Types" (ID)
      );
      ALTER TABLE VEHICLE ADD CONSTRAINT VEHICLE_OWNER FOREIGN KEY (OWNER) REFERENCES DEPOT (ID);
      ALTER TABLE VEHICLE ADD FOREIGN KEY (PREVIOUS_ID) REFERENCES VEHICLE (ID);
      ''', SERVICE).entities()[2]

    then:
    entity.fields() == []
    entity.relations() == [
      new Relation(RelationKind.MANY_TO_ONE, 'Depot', 'depot', 'DEPOT_ID', false),
      new Relation(RelationKind.ONE_TO_ONE, 'Depot', 'homeDepot', 'HOME_DEPOT_ID', true),
      new Relation(RelationKind.MANY_TO_ONE, 'VehicleType', 'type', 'TYPE_ID', true),
      new Relation(RelationKind.MANY_TO_ONE, 'Depot', 'owner', 'OWNER', true),
      new Relation(RelationKind.MANY_TO_ONE, 'Vehicle', 'previous', 'PREVIOUS_ID', true),
    ]
  }

  def "a column may reference the table of an entity of the service"() {
    given:
    Spec service = SpecReader.read('''
      tib: 1
      service: s
      entities:
        - name: Depot
          table: DEPOTS
      '''.stripIndent())

    expect:
    DdlReader.read('CREATE TABLE VEHICLE (ID BIGINT PRIMARY KEY, DEPOT_ID BIGINT REFERENCES depots);', service)
        .entities()*.name() == ['Depot', 'Vehicle']
  }

  def "a column that references a table that is nowhere is a problem"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, X_ID BIGINT REFERENCES X (ID));')

    then:
    DdlException e = thrown()
    e.problems() == ['T.X_ID: there is no table X in the DDL or the service']
  }

  def "a foreign key of several columns is a problem"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, A INT, B INT, FOREIGN KEY (A, B) REFERENCES O (X, Y));')

    then:
    DdlException e = thrown()
    e.problems() == ['T: a foreign key of several columns is not supported']
  }

  def "a key of a column that is not there is a problem"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, UNIQUE (NOPE));')

    then:
    DdlException e = thrown()
    e.problems() == ['T: there is no column NOPE']
  }

  def "#columns make an entity audited: #audit"() {
    when:
    Entity entity = entity("CREATE TABLE T (ID BIGINT PRIMARY KEY, $columns);")

    then:
    entity.audit() == audit
    entity.fields()*.name() == fields

    where:
    columns                                                    | audit | fields
    'CREATED_AT TIMESTAMPTZ, UPDATED_AT TIMESTAMPTZ'           | true  | []
    'created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ'  | true  | []
    'CREATED_AT TIMESTAMPTZ'                                   | false | ['createdAt']
    'UPDATED_AT TIMESTAMPTZ'                                   | false | ['updatedAt']
    'CREATED_AT TIMESTAMP, UPDATED_AT TIMESTAMPTZ'             | false | ['createdAt', 'updatedAt']
    'CREATED_AT TIMESTAMPTZ, UPDATED_AT DATE'                  | false | ['createdAt', 'updatedAt']
  }

  def "an audit column of a type that is not supported is a problem"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, CREATED_AT MONEY, UPDATED_AT TIMESTAMPTZ);')

    then:
    DdlException e = thrown()
    e.problems() == ['T.CREATED_AT: the type MONEY is not supported']
  }

  def "the entity of a table of the service takes its place and keeps its names and options"() {
    given:
    Spec service = SpecReader.read('''
      tib: 1
      service: s
      entities:
        - name: Note
        - name: Car
          table: VEHICLE
          id: { field: key }
          fields:
            - { name: plate, column: PLATE_NUMBER, type: String }
            - { name: gone, type: String }
          relations:
            - { kind: manyToOne, target: Note, field: remark, column: NOTE_ID }
          options: { dto: false }
        - name: Tag
      '''.stripIndent())

    when:
    Spec spec = DdlReader.read('''
      CREATE TABLE DEPOT (ID BIGINT PRIMARY KEY);
      CREATE TABLE vehicle (
        "KEY" BIGINT PRIMARY KEY,
        plate_number VARCHAR(16),
        COLOR VARCHAR(10),
        NOTE_ID BIGINT REFERENCES NOTE,
        DEPOT_ID BIGINT REFERENCES DEPOT
      );
      ''', service)

    then:
    spec.entities()*.name() == ['Note', 'Car', 'Tag', 'Depot']
    Entity car = spec.entities()[1]
    car.table() == 'vehicle'
    car.id().field() == 'key'
    car.fields()*.name() == ['plate', 'color']
    car.fields()*.column() == ['plate_number', 'COLOR']
    car.relations()*.field() == ['remark', 'depot']
    car.options() == new Options(false, true, true, false)
    spec.entities()[0] == service.entities()[0]
  }

  def "the identifier of a table of the service is named after its column when the column is another"() {
    given:
    Spec service = SpecReader.read('tib: 1\nservice: s\nentities:\n  - name: Car\n    table: VEHICLE\n')

    expect:
    DdlReader.read('CREATE TABLE VEHICLE (VEHICLE_ID BIGINT PRIMARY KEY);', service).entities()[0].id().field() == 'vehicleId'
  }

  def "statements other than CREATE TABLE and ALTER TABLE are skipped"() {
    expect:
    DdlReader.read('''
      CREATE SEQUENCE T_SEQ START WITH 1 INCREMENT BY 50;
      DROP TABLE IF EXISTS T;
      CREATE TABLE T (ID BIGINT PRIMARY KEY);
      CREATE INDEX T_ID ON T (ID);
      COMMENT ON TABLE T IS 'A table.';
      INSERT INTO T VALUES (1);
      ''', SERVICE).entities()*.name() == ['T']
  }

  def "#problem"() {
    when:
    DdlReader.read(ddl, SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == [problem]

    where:
    ddl                                                                           | problem
    ''                                                                            | 'there is no CREATE TABLE'
    '-- nothing\n'                                                                | 'there is no CREATE TABLE'
    'CREATE TABLE T (A INT,\n B NUMBER(*, 0));'                                   | 'it is not SQL that tib reads: Encountered: <OPENING_BRACKET> / "(", at line 2, column 10'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nCREATE TABLE t (ID INT PRIMARY KEY);'  | 't: the table is created twice'
    'ALTER TABLE T ADD UNIQUE (A);\nCREATE TABLE T (ID INT PRIMARY KEY, A INT);'  | 'T: ALTER TABLE comes before the table is created'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ADD COLUMN A INT;'       | 'T: ALTER TABLE may only add keys'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T DROP COLUMN ID;'         | 'T: ALTER TABLE may only add keys'
  }

  def "the spec that the DDL gives is checked as a spec"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, A_ID BIGINT REFERENCES T, A BIGINT);')

    then:
    SpecException e = thrown()
    e.problems() == ["entities[0].relations[0].field: 'a' is already a member"]
  }

  private static Entity entity(String ddl) {
    DdlReader.read(ddl, SERVICE).entities()[0]
  }
}
