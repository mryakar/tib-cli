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
package me.yakar.tib.ddl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Spec;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;

/**
 * Reads SQL DDL into the spec of a service. Each table that a {@code CREATE TABLE} statement
 * creates is an entity, named after the table in the singular: its primary key is the identifier, a
 * column that references another table is a relation and every other column a field. {@code ALTER
 * TABLE} may add keys to the tables and make a column an identity; the defaults and the owners that
 * it sets are skipped, and so are row level security, a replica identity and the statistics of a
 * column. Every other statement is skipped without being parsed.
 */
public final class DdlReader {

  private static final Pattern QUOTED = Pattern.compile("^[\"`\\[](.*)[\"`\\]]$");

  /** The first words of a statement that tib parses. */
  private static final Pattern TABLE_STATEMENT =
      Pattern.compile(
          "^(?:CREATE\\s+(?:(?:GLOBAL|LOCAL)\\s+)?(?:(?:TEMP|TEMPORARY|UNLOGGED)\\s+)?|ALTER\\s+)TABLE\\b",
          Pattern.CASE_INSENSITIVE);

  /**
   * The options of the sequence of an identity, which the parser does not read and tib does not
   * need: {@code AS IDENTITY (SEQUENCE NAME ... CACHE 1)}.
   */
  private static final Pattern IDENTITY_OPTIONS =
      Pattern.compile("(?<=\\bAS\\s{1,10}IDENTITY)\\s*\\([^()]*\\)", Pattern.CASE_INSENSITIVE);

  /**
   * A line without anything, which the parser would take for the end of the statement when two come
   * one after the other.
   */
  private static final Pattern BLANK_LINE = Pattern.compile("(?m)^[ \\t\\r]*$");

  /** The line and the column where the parser stopped. */
  private static final Pattern POSITION = Pattern.compile("at line (\\d+), column (\\d+)");

  private DdlReader() {}

  /**
   * Reads DDL into the spec of the service. The entity of a table takes the place of the entity of
   * the same table in the spec, and keeps its name, its options and the names of the fields and
   * relations of the same columns; the entities of the other tables come after those of the spec.
   */
  public static Spec parse(String ddl, Spec service) {
    List<String> problems = new ArrayList<>();
    Map<String, Table> tables = tables(ddl, problems);
    if (tables.isEmpty() && problems.isEmpty()) {
      problems.add("there is no CREATE TABLE");
    }
    Map<String, Entity> known = new LinkedHashMap<>();
    for (Entity entity : service.entities()) {
      known.put(key(entity.table()), entity);
    }
    Map<String, String> names = new HashMap<>();
    known.forEach((table, entity) -> names.put(table, entity.name()));
    tables.forEach(
        (table, read) -> names.putIfAbsent(table, Names.singular(Names.type(read.name))));
    Map<String, Entity> entities = new LinkedHashMap<>();
    tables.forEach(
        (table, read) -> entities.put(table, read.entity(names, known.get(table), problems)));
    if (!problems.isEmpty()) {
      throw new DdlException(problems);
    }
    List<Entity> merged = new ArrayList<>();
    for (Entity entity : service.entities()) {
      merged.add(entities.getOrDefault(key(entity.table()), entity));
    }
    entities.forEach(
        (table, entity) -> {
          if (!known.containsKey(table)) {
            merged.add(entity);
          }
        });
    return new Spec(
            service.tib(),
            service.service(),
            service.group(),
            service.pack(),
            service.java(),
            service.architecture(),
            service.tests(),
            service.docker(),
            merged)
        .check();
  }

  /**
   * The tables that the DDL creates, by the keys of their names, with what ALTER TABLE gives them.
   */
  private static Map<String, Table> tables(String ddl, List<String> problems) {
    Map<String, Table> tables = new LinkedHashMap<>();
    for (Statement statement : statements(ddl, problems)) {
      if (statement instanceof CreateTable create) {
        if (create.getColumnDefinitions() == null || create.getLikeTable() != null) {
          problems.add(
              unquote(create.getTable().getName())
                  + ": a table that is created from another one is not supported");
          continue;
        }
        Table table = new Table(create, problems);
        if (tables.putIfAbsent(key(table.name), table) != null) {
          problems.add(table.name + ": the table is created twice");
        }
      } else {
        // The statements create or alter tables, and the parser reads ALTER TABLE as an Alter.
        Alter alter = (Alter) statement;
        String name = unquote(alter.getTable().getName());
        Table table = tables.get(key(name));
        if (table == null && alter.getAlterExpressions().stream().allMatch(Table::skipped)) {
          // An older pg_dump gives a sequence or a view its owner with ALTER TABLE; what says
          // nothing of a table says nothing of what is no table.
          continue;
        }
        if (table == null) {
          problems.add(name + ": ALTER TABLE comes before the table is created");
        } else {
          table.alter(alter, problems);
        }
      }
    }
    return tables;
  }

  /**
   * The statements of the DDL that create or alter a table, parsed one by one; a statement that the
   * parser cannot read is a problem, at its line and column in the DDL, and so are rows of a table
   * that no line ends: the statements after them would be lost.
   */
  private static List<Statement> statements(String ddl, List<String> problems) {
    List<Statement> statements = new ArrayList<>();
    for (Script.Statement statement : Script.split(ddl)) {
      if (statement.rowsWithoutEnd()) {
        problems.add(
            "the rows after the COPY at line "
                + statement.line()
                + " have no line \\. that ends them");
      }
      if (!TABLE_STATEMENT.matcher(statement.text()).find()) {
        continue;
      }
      try {
        statements.add(CCJSqlParserUtil.parse(parseable(statement.text())));
      } catch (JSQLParserException e) {
        problems.add("it is not SQL that tib reads: " + reason(e, statement));
      }
    }
    return statements;
  }

  /**
   * The statement as the parser reads it, with everything that it reads at its line and column:
   * blanks in the place of the options of an identity, and a comment on every line without
   * anything.
   */
  private static String parseable(String statement) {
    return BLANK_LINE
        .matcher(
            IDENTITY_OPTIONS
                .matcher(statement)
                .replaceAll(options -> options.group().replaceAll("[^\\n]", " ")))
        .replaceAll("--");
  }

  /**
   * What the parser met where it stopped, as in {@code Encountered: "(", at line 2, column 10},
   * with the line and the column in the DDL.
   */
  private static String reason(JSQLParserException e, Script.Statement statement) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String message = cause.getMessage().lines().findFirst().orElse("");
    return message
            .replaceFirst("^.*?(Encountered)", "$1")
            .replaceFirst(", at line \\d+, column \\d+.*$", "")
        + inDdl(message, statement);
  }

  /**
   * The line and the column that the message of the parser names in the statement, as those in the
   * DDL: {@code , at line 2, column 10}; nothing when the message names none.
   */
  private static String inDdl(String message, Script.Statement statement) {
    return POSITION
        .matcher(message)
        .results()
        .findFirst()
        .map(
            at ->
                ", at "
                    + statement.at(Integer.parseInt(at.group(1)), Integer.parseInt(at.group(2))))
        .orElse("");
  }

  /** The key of an SQL name, under which the same name in another case is the same. */
  static String key(String name) {
    return unquote(name).toUpperCase(Locale.ROOT);
  }

  /**
   * An SQL name without the quotes around it, which may be double quotes, backticks or brackets.
   */
  static String unquote(String name) {
    return QUOTED.matcher(name).replaceFirst("$1");
  }
}
