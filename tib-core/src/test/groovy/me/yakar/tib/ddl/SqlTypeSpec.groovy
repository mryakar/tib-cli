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

import me.yakar.tib.spec.FieldType
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.create.table.ColDataType
import net.sf.jsqlparser.statement.create.table.CreateTable
import spock.lang.Specification

class SqlTypeSpec extends Specification {

  def "the SQL type #sql is a #type"() {
    when:
    SqlType sqlType = SqlType.of(type(sql))

    then:
    sqlType.type() == type
    sqlType.length() == length
    sqlType.precision() == precision
    sqlType.scale() == scale

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

  def "the SQL type #sql is not supported, and is written #text"() {
    expect:
    SqlType.of(type(sql)) == null
    SqlType.text(type(sql)) == text

    where:
    sql                | text
    'MONEY'            | 'MONEY'
    'DOUBLE PRECISION' | 'DOUBLE PRECISION'
    'INTEGER[]'        | 'INTEGER[]'
    'INT[3][]'         | 'INT[3][]'
    'VARCHAR(MAX)'     | 'VARCHAR(MAX)'
  }

  def "a serial type has the database assign the value: #sql"() {
    expect:
    SqlType.of(type(sql)).serial() == serial

    where:
    sql           | serial
    'SERIAL'      | true
    'SMALLSERIAL' | true
    'BIGSERIAL'   | true
    'BIGINT'      | false
  }

  /** The type of a column as the parser reads it. */
  private static ColDataType type(String sql) {
    ((CreateTable) CCJSqlParserUtil.parse("CREATE TABLE T (F $sql)")).columnDefinitions[0].colDataType
  }
}
