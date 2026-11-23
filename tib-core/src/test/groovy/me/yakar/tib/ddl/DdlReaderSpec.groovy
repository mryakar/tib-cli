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
import me.yakar.tib.spec.Architecture
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

class DdlReaderSpec extends Specification {

  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  static final Spec SERVICE = SpecReader.parse('tib: 1\nservice: vehicle-service\ngroup: me.yakar\n')

  /** The depot of the example DDL, which the output of pg_dump has too. */
  static final Entity DEPOT = new Entity('Depot', 'depots', new Id('id', 'id', IdType.UUID, IdStrategy.UUID), [
    new Field('code', 'code', FieldType.STRING, 8, null, null, false, true),
    new Field('openedOn', 'opened_on', FieldType.LOCAL_DATE, null, null, null, true, false),
  ], [], false, null)

  /** The vehicle of the example DDL, which the output of pg_dump has too. */
  static final Entity VEHICLE = new Entity('Vehicle', 'vehicles', new Id('id', 'id', IdType.LONG, IdStrategy.IDENTITY), [
    new Field('plateNumber', 'plate_number', FieldType.STRING, 16, null, null, false, true),
    new Field('capacity', 'capacity', FieldType.INTEGER, null, null, null, true, false),
    new Field('price', 'price', FieldType.BIG_DECIMAL, null, 12, 2, true, false),
  ], [new Relation(RelationKind.MANY_TO_ONE, 'Depot', 'depot', 'depot_id', true)], true, null)

  def "reads the tables of the example DDL into the spec of the service"() {
    when:
    Spec spec = DdlReader.parse(Files.readString(EXAMPLES.resolve('vehicle-service.sql')), SERVICE)

    then:
    spec.service() == 'vehicle-service'
    spec.group() == 'me.yakar'
    spec.entities() == [DEPOT, VEHICLE,]
  }

  def "the DDL keeps the Java, the architecture, the tests and the Dockerfile of the service"() {
    when:
    Spec spec = DdlReader.parse('CREATE TABLE T (ID BIGINT PRIMARY KEY);', SpecReader.parse('tib: 1\nservice: s\njava: 25\ntests: [unit]\ndocker: true\n'))

    then:
    spec.java() == 25
    spec.architecture() == Architecture.ANEMIC
    spec.tests()*.toString() == ['unit']
    spec.docker()
  }

  def "a column of a supported SQL type is a field of its type, with its sizes"() {
    expect:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, F NUMERIC(12, 2));').fields() == [new Field('f', 'F', FieldType.BIG_DECIMAL, null, 12, 2, true, false)]
  }

  def "a column of an SQL type that is not supported is a problem"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, F INTEGER[]);')

    then:
    DdlException e = thrown()
    e.problems() == ['T.F: the type INTEGER[] is not supported']
    e.message == 'the DDL has a problem: T.F: the type INTEGER[] is not supported'
  }

  def "a table that is created from another one is a problem: #sql"() {
    when:
    DdlReader.parse("CREATE TABLE S (ID BIGINT PRIMARY KEY);\n$sql;", SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == ['T: a table that is created from another one is not supported']

    where:
    sql << [
      'CREATE TABLE T AS SELECT * FROM S',
      'CREATE TABLE T LIKE S',
      'CREATE TABLE T (LIKE S)',
      'CREATE TABLE T (LIKE public.S INCLUDING ALL, EXTRA INT PRIMARY KEY)',
      'CREATE TABLE T (EXTRA INT PRIMARY KEY, LIKE S)',
    ]
  }

  def "a keyword in a string literal or in an expression says nothing of the column: #literal"() {
    expect:
    entity("CREATE TABLE T (ID BIGINT PRIMARY KEY $literal, A INT $literal);") == new Entity(
        'T', 'T', new Id('id', null, IdType.LONG, IdStrategy.SEQUENCE),
        [new Field('a', 'A', FieldType.INTEGER, null, null, null, true, false)],
        [], false, null)

    where:
    literal << [
      "COMMENT 'NOT NULL'",
      "DEFAULT 'not null'",
      "COMMENT 'not unique, may be null'",
      "COMMENT 'the primary key'",
      "COMMENT 'an identity, or auto_increment'",
      "COMMENT 'references the order'",
      "COMMENT 'it''s unique'",
      "DEFAULT 'nextval(1)'",
      'CHECK (ID IS NOT NULL OR (A IS NOT NULL AND A > 0))',
      'GENERATED ALWAYS AS (CASE WHEN A IS NOT NULL THEN 1 END) STORED',
    ]
  }

  def "the primary key #sql is an identifier of #type by #strategy"() {
    expect:
    entity("CREATE TABLE T (ID $sql);").id() == new Id('id', null, type, strategy)

    where:
    sql                                                | type        | strategy
    'BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY' | IdType.LONG | IdStrategy.IDENTITY
    'BIGINT AUTO_INCREMENT PRIMARY KEY'                | IdType.LONG | IdStrategy.IDENTITY
    'BIGSERIAL PRIMARY KEY'                            | IdType.LONG | IdStrategy.IDENTITY
    'SERIAL PRIMARY KEY'                               | IdType.LONG | IdStrategy.IDENTITY
    "INTEGER DEFAULT nextval('T_SEQ'::regclass) PRIMARY KEY" | IdType.LONG | IdStrategy.IDENTITY
    "INTEGER DEFAULT (nextval('T_SEQ')) PRIMARY KEY"    | IdType.LONG | IdStrategy.IDENTITY
    'NUMBER(19) PRIMARY KEY'                           | IdType.LONG | IdStrategy.SEQUENCE
    'INT PRIMARY KEY'                                  | IdType.LONG | IdStrategy.SEQUENCE
    'UUID PRIMARY KEY'                                 | IdType.UUID | IdStrategy.UUID
  }

  def "the identifier is named after its column #column, which it keeps"() {
    expect:
    entity("CREATE TABLE T ($column BIGINT, CONSTRAINT T_PK PRIMARY KEY ($column));").id() ==
        new Id(field, column, IdType.LONG, IdStrategy.SEQUENCE)

    where:
    column        | field
    'T_ID'        | 'tId'
    'Vehicle_Key' | 'vehicleKey'
  }

  def "a primary key of the type #sql is a problem"() {
    when:
    entity("CREATE TABLE T (ID $sql PRIMARY KEY);")

    then:
    DdlException e = thrown()
    e.problems() == ["T.ID: the primary key is $sql, and only an integer or a UUID is supported"]

    where:
    sql << ['VARCHAR(10)', 'MONEY']
  }

  def "a name of underscores alone, which no Java name comes of, is a problem"() {
    when:
    DdlReader.parse('''
      CREATE TABLE __ (ID BIGINT PRIMARY KEY);
      CREATE TABLE T (_ BIGINT PRIMARY KEY, __ INT, ___ID BIGINT REFERENCES T);
      ''', SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == [
      '__: a name of underscores alone is not supported',
      'T._: a name of underscores alone is not supported',
      'T.__: a name of underscores alone is not supported',
    ]
  }

  def "a table without a primary key, or with one of several columns, is a problem"() {
    when:
    DdlReader.parse('CREATE TABLE A (X INT);\nCREATE TABLE B (X INT, Y INT, PRIMARY KEY (X, Y));', SERVICE)

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
    Entity entity = DdlReader.parse('''
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

  def "the relation of the column #column is named #field"() {
    expect:
    entity("CREATE TABLE T (ID BIGINT PRIMARY KEY, $column BIGINT REFERENCES T);").relations()*.field() == [field]

    where:
    column   | field
    'b_id'   | 'b'
    'b__id'  | 'b'
    'B_C_ID' | 'bC'
    'b_idx'  | 'bIdx'
  }

  def "the column #column of a relation, which has no name before its suffix, keeps the suffix"() {
    when:
    entity("CREATE TABLE T (ID BIGINT PRIMARY KEY, $column BIGINT REFERENCES T);")

    then: 'the relation is named id, as the identifier is'
    SpecException e = thrown()
    e.problems() == ["entities[0].relations[0].field: 'id' is already a member"]

    where:
    column << ['_id', '__ID']
  }

  def "a column may reference the table of an entity of the service"() {
    given:
    Spec service = SpecReader.parse('''
      tib: 1
      service: s
      entities:
        - name: Depot
          table: DEPOTS
      '''.stripIndent())

    expect:
    DdlReader.parse('CREATE TABLE VEHICLE (ID BIGINT PRIMARY KEY, DEPOT_ID BIGINT REFERENCES depots);', service)
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
    Spec service = SpecReader.parse('''
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
    Spec spec = DdlReader.parse('''
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
    car.options() == new Options(false, true, true)
    spec.entities()[0] == service.entities()[0]
  }

  def "the identifier of a table of the service is named after its column when the column is another"() {
    given:
    Spec service = SpecReader.parse('tib: 1\nservice: s\nentities:\n  - name: Car\n    table: VEHICLE\n')

    expect:
    DdlReader.parse('CREATE TABLE VEHICLE (VEHICLE_ID BIGINT PRIMARY KEY);', service).entities()[0].id().field() == 'vehicleId'
  }

  def "statements other than CREATE TABLE and ALTER TABLE are skipped without being parsed"() {
    expect:
    DdlReader.parse('''
      \\restrict key
      CREATE SEQUENCE T_SEQ START WITH 1 INCREMENT BY 50;
      ALTER SEQUENCE T_SEQ OWNER TO postgres;
      DROP TABLE IF EXISTS T;
      CREATE UNLOGGED TABLE T (ID BIGINT PRIMARY KEY);
      CREATE INDEX T_ID ON T (ID);
      COMMENT ON TABLE T IS 'A table; with a semicolon.';
      INSERT INTO T VALUES (1);
      CREATE FUNCTION F() RETURNS trigger LANGUAGE plpgsql AS $$
      BEGIN
        IF NEW.ID > 0 THEN RETURN NEW; END IF;
      END;
      $$;
      THIS IS NOT SQL;
      \\unrestrict key
      ''', SERVICE).entities()*.name() == ['T']
  }

  def "lines without anything in a statement do not end it"() {
    expect:
    entity('CREATE TABLE T (\n  ID BIGINT PRIMARY KEY,\n\n\n  A INT,\r\n \t\r\n\r\n  B INT\n);').fields()*.name() == ['a', 'b']
  }

  def "ALTER TABLE may set a default or an owner, which say nothing of the entity"() {
    expect:
    entity('''
      ALTER TABLE T_SEQ OWNER TO postgres;
      CREATE TABLE T (ID INT NOT NULL, A INT);
      ALTER TABLE T OWNER TO "Some One";
      ALTER TABLE ONLY T ALTER COLUMN ID SET DEFAULT 'nextval(1)';
      ALTER TABLE ONLY T ALTER COLUMN A SET DEFAULT 0;
      ALTER TABLE ONLY T ADD CONSTRAINT T_PK PRIMARY KEY (ID);
      ''') == new Entity(
        'T', 'T', new Id('id', null, IdType.LONG, IdStrategy.SEQUENCE),
        [new Field('a', 'A', FieldType.INTEGER, null, null, null, true, false)],
        [], false, null)
  }

  def "the rows of a dump with data are skipped, and the keys after them are read"() {
    expect:
    entity('''
      CREATE TABLE T (ID INT NOT NULL, NAME TEXT);
      COPY T (ID, NAME) FROM stdin;
1\tO'Brien; CREATE TABLE U (ID INT);
2\t$$
\\.
      ALTER TABLE ONLY T ADD CONSTRAINT T_PK PRIMARY KEY (ID);
      ''') == new Entity(
        'T', 'T', new Id('id', null, IdType.LONG, IdStrategy.SEQUENCE),
        [new Field('name', 'NAME', FieldType.TEXT, null, null, null, true, false)],
        [], false, null)
  }

  def "rows of a dump that no line ends are a problem, as the statements after them are lost"() {
    when:
    DdlReader.parse("CREATE TABLE T (ID INT PRIMARY KEY);\n\nCOPY T (ID) FROM stdin;\n1\n2\nCREATE TABLE U (ID INT PRIMARY KEY);\n", SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == ['the rows after the COPY at line 3 have no line \\. that ends them']
  }

  def "ALTER TABLE that says nothing of the entity is skipped: #change"() {
    expect:
    entity("""
      ALTER TABLE ONLY OTHER $change;
      CREATE TABLE T (ID INT PRIMARY KEY, A INT);
      ALTER TABLE ONLY public.T $change;
      """) == new Entity(
        'T', 'T', new Id('id', null, IdType.LONG, IdStrategy.SEQUENCE),
        [new Field('a', 'A', FieldType.INTEGER, null, null, null, true, false)],
        [], false, null)

    where:
    change << [
      'ENABLE ROW LEVEL SECURITY',
      'DISABLE ROW LEVEL SECURITY',
      'FORCE ROW LEVEL SECURITY',
      'NO FORCE ROW LEVEL SECURITY',
      'REPLICA IDENTITY FULL',
      'REPLICA IDENTITY NOTHING',
      'REPLICA IDENTITY DEFAULT',
      'REPLICA IDENTITY USING INDEX T_PKEY',
      'ALTER COLUMN A SET STATISTICS 1000',
      'ALTER A SET STATISTICS -1',
      'ALTER COLUMN "No Such" SET STATISTICS 0',
    ]
  }

  def "a default of the next value of a sequence that ALTER TABLE sets makes the column an identity"() {
    expect:
    entity('''
      CREATE TABLE T (ID INT NOT NULL PRIMARY KEY);
      ALTER TABLE ONLY T ALTER COLUMN ID SET DEFAULT nextval('T_SEQ'::regclass);
      ''').id() == new Id('id', null, IdType.LONG, IdStrategy.IDENTITY)
  }

  def "ALTER TABLE may make the column of the primary key an identity: #how"() {
    expect:
    entity("CREATE TABLE T (ID BIGINT PRIMARY KEY);\nALTER TABLE T ALTER COLUMN ID ADD GENERATED $how AS IDENTITY$options;")
        .id() == new Id('id', null, IdType.LONG, IdStrategy.IDENTITY)

    where:
    how          | options
    'ALWAYS'     | ''
    'BY DEFAULT' | ' (\n    SEQUENCE NAME T_ID_SEQ\n    START WITH 1\n    CACHE 1\n)'
  }

  def "reads the output of pg_dump --schema-only"() {
    when:
    Spec spec = DdlReader.parse(Files.readString(EXAMPLES.resolve('pg-dump-18.sql')), SERVICE)

    then:
    spec.entities() == [
      DEPOT,
      new Entity('Driver', 'drivers', new Id('id', 'id', IdType.LONG, IdStrategy.IDENTITY), [new Field('name', 'name', FieldType.TEXT, null, null, null, false, false),], [new Relation(RelationKind.ONE_TO_ONE, 'Vehicle', 'vehicle', 'vehicle_id', true)], false, null),
      VEHICLE,
    ]
  }

  def "a DDL that tib cannot use is refused: #problem"() {
    when:
    DdlReader.parse(ddl, SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == [problem]

    where:
    ddl                                                                           | problem
    ''                                                                            | 'there is no CREATE TABLE'
    '-- nothing\n'                                                                | 'there is no CREATE TABLE'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nCREATE TABLE t (ID INT PRIMARY KEY);'  | 't: the table is created twice'
    'ALTER TABLE T ADD UNIQUE (A);\nCREATE TABLE T (ID INT PRIMARY KEY, A INT);'  | 'T: ALTER TABLE comes before the table is created'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ADD COLUMN A INT;'       | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T DROP COLUMN ID;'         | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ALTER COLUMN ID TYPE BIGINT;' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ALTER COLUMN ID DROP DEFAULT;' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T SET SCHEMA S;'           | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ATTACH PARTITION P FOR VALUES FROM (1) TO (2);' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ENABLE TRIGGER X;' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T CLUSTER ON T_PKEY;' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ALTER COLUMN ID SET STORAGE EXTERNAL;' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T DISABLE ROW LEVEL SECURITY, ADD COLUMN A INT;' | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T RENAME TO U;'           | 'T: ALTER TABLE may only add keys, make a column an identity, or set a default or the owner'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ALTER COLUMN X ADD GENERATED ALWAYS AS IDENTITY;' | 'T: there is no column X'
    'CREATE TABLE T (ID INT PRIMARY KEY);\nALTER TABLE T ALTER COLUMN X SET DEFAULT 0;' | 'T: there is no column X'
    'ALTER TABLE T OWNER TO ME;\nALTER TABLE T ADD UNIQUE (ID);\nCREATE TABLE T (ID INT PRIMARY KEY);' | 'T: ALTER TABLE comes before the table is created'
  }

  def "each table statement that the parser cannot read is a problem, at its line and column in the DDL"() {
    when:
    DdlReader.parse('''CREATE TABLE A (ID INT PRIMARY KEY);
      CREATE TABLE B (ID INT PRIMARY KEY,
        X NUMBER(*, 0));
      ALTER TABLE A ADD CONSTRAINT C CHECK ((;
      CREATE TABLE C (ID INT \\ PRIMARY KEY);''', SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == [
      'it is not SQL that tib reads: Encountered: <OPENING_BRACKET> / "(", at line 3, column 17',
      'it is not SQL that tib reads: Encountered: <EOF>, at line 4, column 45',
      'it is not SQL that tib reads: Encountered: "\\\\" (92) after: "", at line 5, column 30',
    ]
  }

  def "what comes after the options of an identity is at its line and column: #problem"() {
    when:
    DdlReader.parse("CREATE TABLE T (\n  ID BIGINT GENERATED ALWAYS AS IDENTITY $options PRIMARY KEY, X INT[ );", SERVICE)

    then:
    DdlException e = thrown()
    e.problems() == ["it is not SQL that tib reads: $problem"]

    where:
    options                                          | problem
    '(START WITH 1 CACHE 1)'                         | 'Encountered: "[" / "[", at line 2, column 83'
    '(\n    SEQUENCE NAME T_ID_SEQ\n    CACHE 1\n  )' | 'Encountered: "[" / "[", at line 5, column 23'
  }

  def "the spec that the DDL gives is checked as a spec"() {
    when:
    entity('CREATE TABLE T (ID BIGINT PRIMARY KEY, A_ID BIGINT REFERENCES T, A BIGINT);')

    then:
    SpecException e = thrown()
    e.problems() == ["entities[0].relations[0].field: 'a' is already a member"]
  }

  private static Entity entity(String ddl) {
    DdlReader.parse(ddl, SERVICE).entities()[0]
  }
}
