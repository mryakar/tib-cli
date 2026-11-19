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

import spock.lang.Specification

class ScriptSpec extends Specification {

  def "a semicolon ends a statement, which starts at its line and column"() {
    expect:
    Script.split('CREATE TABLE A (ID INT);\n  CREATE TABLE B (ID INT) ;\r\nC') == [
      new Script.Statement('CREATE TABLE A (ID INT)', 1, 1),
      new Script.Statement('CREATE TABLE B (ID INT)', 2, 3),
      new Script.Statement('C', 3, 1),
    ]
  }

  def "line #line, column #column of a statement is at #at of the script"() {
    expect:
    new Script.Statement('B (\n  X', 3, 5).at(line, column) == at

    where:
    line | column | at
    1    | 1      | 'line 3, column 5'
    1    | 3      | 'line 3, column 7'
    2    | 3      | 'line 4, column 3'
  }

  def "a semicolon in #where does not end the statement"() {
    expect:
    Script.split("$sql;\nB")*.text() == [sql.stripTrailing(), 'B']

    where:
    where                            | sql
    'a string'                       | "SELECT 'a;b'"
    'a string with a doubled quote'  | "SELECT 'it''s; so'"
    'a string with escapes'          | "SELECT E'a\\';b'"
    'a string with escapes, as e'    | "SELECT e'a\\';b'"
    'a quoted name'                  | 'SELECT "a;""b"'
    'a dollar-quoted body'           | 'SELECT $$a;b$$'
    'a body that starts the statement' | '$$a;b$$'
    'a body with a tag'              | 'SELECT $body$ a; $$ b; $body$'
    'a body with a tag of any letters' | 'SELECT $gövde1$ a; $$ b; $gövde1$'
    'a line comment'                 | 'SELECT 1 -- a; b\n'
    'a block comment'                | 'SELECT 1 /* a; b */'
    'a comment in a block comment'   | 'SELECT 1 /* a; /* b; */ c; /* d */ e; */'
  }

  def "a backslash is an escape only in a string that starts with E: #sql"() {
    expect:
    Script.split(sql)*.text() == texts

    where:
    sql                    | texts
    "E'\\';x';y"           | ["E'\\';x'", 'y']
    "SELECT name'\\';x';y" | ["SELECT name'\\'", "x';y"]
    "'\\';x"               | ["'\\'", 'x']
    "'a'"                  | ["'a'"]
  }

  def "a dollar sign that opens no body is part of the statement: #sql"() {
    expect:
    Script.split(sql)*.text() == texts

    where:
    sql                | texts
    'SELECT $1; B'     | ['SELECT $1', 'B']
    'SELECT x$y$; B'   | ['SELECT x$y$', 'B']
    'SELECT $1$; B'    | ['SELECT $1$', 'B']
    'SELECT _$y$; B'   | ['SELECT _$y$', 'B']
    'SELECT x$$; B'    | ['SELECT x$$', 'B']
  }

  def "a backslash between statements starts a command of psql, which runs to the end of its line"() {
    expect:
    Script.split(sql) == [new Script.Statement('SET A = 1', 2, 1)]

    where:
    sql << [
      '\\restrict key\nSET A = 1;\n  \\unrestrict key\n',
      '/* c */ \\connect db; \\x\nSET A = 1; \\d\n-- c\n\\q',
    ]
  }

  def "a backslash in a statement is part of the statement"() {
    expect:
    Script.split('SELECT 1\n\\x\n;') == [new Script.Statement('SELECT 1\n\\x', 1, 1)]
  }

  def "the rows after COPY FROM stdin are no statements: #rows"() {
    expect:
    Script.split("A;\n${copy};\n${rows}\\.\nB;") == [
      new Script.Statement('A', 1, 1),
      new Script.Statement(copy, 2, 1),
      new Script.Statement('B', 4 + copy.count('\n') + rows.count('\n'), 1),
    ]

    where:
    copy                                        | rows
    'COPY public.t (id, name) FROM stdin'       | "1\tO'Brien\n"
    'copy t from STDIN'                         | '1\ta; b\n2\tc;\n'
    'COPY t FROM stdin WITH (FORMAT csv)'       | '1,$$\n2,$a$\n'
    'COPY "T" (a)\n  FROM\n  stdin'             | '\\.x\n \\.\n\\N\n/* --\n'
    'COPY t FROM stdin'                         | ''
    'COPY t FROM stdin'                         | '1\r\n2\r\n'
  }

  def "rows that no line ends run to the end of the script"() {
    expect:
    Script.split("COPY t FROM stdin;\n1\tO'Brien\nCREATE TABLE A (ID INT);")*.text() == ['COPY t FROM stdin']
    Script.split('COPY t FROM stdin;')*.text() == ['COPY t FROM stdin']
  }

  def "no rows follow #copy"() {
    expect:
    Script.split("$copy;\nB;\n\\.\nC")*.text() == [copy, 'B', 'C']

    where:
    copy << [
      "COPY t FROM '/tmp/stdin'",
      'COPY t TO stdout',
      "COPY (SELECT ' FROM stdin') TO stdout",
      'SELECT copy FROM stdin',
    ]
  }

  def "comments and blanks are no statements: #sql"() {
    expect:
    Script.split(sql) == []

    where:
    sql << [
      '',
      ' \n',
      ';;',
      '-- only a comment',
      '/* a */\n-- b\n;',
      '/* never closed; ',
      '/* a /* closed */ never closed; ',
      '/* a; /* b; */ c; */'
    ]
  }

  def "a string, a name or a body that is not closed runs to the end: #sql"() {
    expect:
    Script.split(sql)*.text() == [sql]

    where:
    sql << ["SELECT 'a;b", 'SELECT "a;b', 'SELECT $$a;b', "SELECT E'a\\"]
  }
}
