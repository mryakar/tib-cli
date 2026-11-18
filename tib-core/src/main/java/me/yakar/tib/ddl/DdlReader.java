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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.Field;
import me.yakar.tib.spec.FieldType;
import me.yakar.tib.spec.Id;
import me.yakar.tib.spec.IdStrategy;
import me.yakar.tib.spec.IdType;
import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Relation;
import me.yakar.tib.spec.RelationKind;
import me.yakar.tib.spec.Spec;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

/**
 * Reads SQL DDL into the spec of a service. Each table that a {@code CREATE TABLE} statement
 * creates is an entity, named after the table in the singular: its primary key is the identifier, a
 * column that references another table is a relation and every other column a field. {@code ALTER
 * TABLE} may add keys to the tables, make a column an identity, set a default or an owner, as the
 * output of {@code pg_dump --schema-only} does; other statements, such as those that create
 * sequences, indexes, functions or comments, are skipped without being parsed.
 */
public final class DdlReader {

  private static final Pattern NOT_NULL =
      Pattern.compile("\\bNOT\\s+NULL\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern PRIMARY_KEY =
      Pattern.compile("\\bPRIMARY\\s+KEY\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern UNIQUE = Pattern.compile("\\bUNIQUE\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern IDENTITY =
      Pattern.compile("\\b(IDENTITY|AUTO_INCREMENT)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern QUOTED = Pattern.compile("^[\"`\\[](.*)[\"`\\]]$");
  private static final Pattern REFERENCES =
      Pattern.compile("\\bREFERENCES\\s+([^\\s(]+)", Pattern.CASE_INSENSITIVE);

  /** A string literal, which the keywords above are never found in. */
  private static final Pattern LITERAL = Pattern.compile("'(?:[^']|'')*'");

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

  /** A column that an {@code ALTER TABLE} makes an identity, as {@code ID ADD GENERATED ... }. */
  private static final Pattern ADD_IDENTITY =
      Pattern.compile(
          "^\\S+\\s+ADD\\s+GENERATED\\s+(?:ALWAYS|BY\\s+DEFAULT)\\s+AS\\s+IDENTITY$",
          Pattern.CASE_INSENSITIVE);

  /** The owner that an {@code ALTER TABLE} gives the table. */
  private static final Pattern OWNER = Pattern.compile("^OWNER\\s+TO\\b", Pattern.CASE_INSENSITIVE);

  /** The suffix of the column of a relation, after the name of the relation. */
  private static final Pattern ID_SUFFIX = Pattern.compile("(?i)(.)_ID$");

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
    return new Spec(service.tib(), service.service(), service.group(), service.pack(), merged)
        .check();
  }

  /** The tables that the DDL creates, by their keys, with the keys that it adds to them. */
  private static Map<String, Table> tables(String ddl, List<String> problems) {
    Map<String, Table> tables = new LinkedHashMap<>();
    for (Statement statement : statements(ddl, problems)) {
      if (statement instanceof CreateTable create) {
        if (create.getColumnDefinitions() == null) {
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
        if (table == null && alter.getAlterExpressions().stream().allMatch(DdlReader::owner)) {
          // pg_dump gives sequences their owner with ALTER TABLE too.
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
   * parser cannot read is a problem, at its line and column in the DDL.
   */
  private static List<Statement> statements(String ddl, List<String> problems) {
    List<Statement> statements = new ArrayList<>();
    for (Script.Statement statement : Script.split(ddl)) {
      if (!TABLE_STATEMENT.matcher(statement.text()).find()) {
        continue;
      }
      String sql =
          BLANK_LINE
              .matcher(
                  IDENTITY_OPTIONS
                      .matcher(statement.text())
                      .replaceAll(options -> options.group().replaceAll("[^\\n]", "")))
              .replaceAll("--");
      try {
        statements.add(CCJSqlParserUtil.parse(sql));
      } catch (JSQLParserException e) {
        problems.add("it is not SQL that tib reads: " + reason(e, statement));
      }
    }
    return statements;
  }

  /** Whether the change gives the table an owner, which tib does not need. */
  private static boolean owner(AlterExpression expression) {
    return expression.getOperation() == AlterOperation.UNSPECIFIC
        && OWNER.matcher(String.valueOf(expression.getOptionalSpecifier())).find();
  }

  /**
   * Where the parser stopped in the statement, as in {@code Encountered: "(", at line 2, column
   * 10}, with the line and the column in the DDL.
   */
  private static String reason(JSQLParserException e, Script.Statement statement) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause
        .getMessage()
        .lines()
        .findFirst()
        .orElse("")
        .replaceFirst("^.*?(Encountered)", "$1")
        .replaceFirst(", in lexical state.*$", "")
        .transform(reason -> inDdl(reason, statement));
  }

  /** The reason with the line and the column in the statement as those in the DDL. */
  private static String inDdl(String reason, Script.Statement statement) {
    Matcher at = POSITION.matcher(reason);
    if (!at.find()) {
      return reason;
    }
    int line = Integer.parseInt(at.group(1));
    int column = Integer.parseInt(at.group(2));
    return at.replaceFirst(
        "at line "
            + (statement.line() + line - 1)
            + ", column "
            + (line == 1 ? statement.column() + column - 1 : column));
  }

  /** The key of an SQL name, under which the same name in another case is the same. */
  private static String key(String name) {
    return unquote(name).toUpperCase(Locale.ROOT);
  }

  /**
   * An SQL name without the quotes around it, which may be double quotes, backticks or brackets.
   */
  private static String unquote(String name) {
    return QUOTED.matcher(name).replaceFirst("$1");
  }

  /**
   * The names of the members of the entity of the spec, by the keys of their columns: a column that
   * the spec has keeps the name the spec gives it.
   */
  private static Map<String, String> members(Entity known) {
    Map<String, String> members = new HashMap<>();
    if (known != null) {
      members.put(key(known.id().column()), known.id().field());
      known.fields().forEach(field -> members.put(key(field.column()), field.name()));
      known.relations().forEach(relation -> members.put(key(relation.column()), relation.field()));
    }
    return members;
  }

  /** A table that the DDL creates, with the keys that it gives the table. */
  private static final class Table {

    final String name;
    final Map<String, Column> columns = new LinkedHashMap<>();
    List<String> primaryKey = List.of();

    Table(CreateTable create, List<String> problems) {
      name = unquote(create.getTable().getName());
      for (ColumnDefinition definition : create.getColumnDefinitions()) {
        Column column = new Column(definition);
        columns.put(key(column.name), column);
        if (column.primary) {
          primaryKey = List.of(column.name);
        }
      }
      for (Index index : create.getIndexes()) {
        take(index, problems);
      }
    }

    /**
     * Takes the keys that the statement adds to the table and the columns that it makes identities,
     * and skips the defaults and the owner that it sets; any other change is a problem.
     */
    void alter(Alter alter, List<String> problems) {
      for (AlterExpression expression : alter.getAlterExpressions()) {
        if (expression.getOperation() == AlterOperation.ADD && expression.getIndex() != null) {
          take(expression.getIndex(), problems);
        } else if (owner(expression) || expression.getColumnSetDefaultList() != null) {
          // The defaults of the columns and the owner of the table say nothing of the entity.
        } else if (identities(expression)) {
          for (AlterExpression.ColumnDataType identity : expression.getColDataTypeList()) {
            String column = unquote(identity.getColumnName());
            if (columns.containsKey(key(column))) {
              column(column).identity = true;
            } else {
              problems.add(name + ": there is no column " + column);
            }
          }
        } else {
          problems.add(
              name
                  + ": ALTER TABLE may only add keys, make a column an identity, or set a default"
                  + " or the owner");
        }
      }
    }

    /** Whether each column that the change alters becomes an identity. */
    private static boolean identities(AlterExpression expression) {
      return expression.getOperation() == AlterOperation.ALTER
          && expression.getColDataTypeList() != null
          && expression.getColDataTypeList().stream()
              .allMatch(column -> ADD_IDENTITY.matcher(column.toString()).find());
    }

    /** Takes a primary, foreign or unique key; a unique key of several columns is not kept. */
    void take(Index index, List<String> problems) {
      List<String> names = index.getColumnsNames().stream().map(DdlReader::unquote).toList();
      for (String column : names) {
        if (!columns.containsKey(key(column))) {
          problems.add(name + ": there is no column " + column);
          return;
        }
      }
      // A check constraint has no type.
      String type = String.valueOf(index.getType()).toUpperCase(Locale.ROOT);
      if (index instanceof ForeignKeyIndex foreignKey) {
        if (names.size() > 1) {
          problems.add(name + ": a foreign key of several columns is not supported");
        } else {
          column(names.get(0)).references = unquote(foreignKey.getTable().getName());
        }
      } else if (type.equals("PRIMARY KEY")) {
        primaryKey = names;
      } else if (type.startsWith("UNIQUE") && names.size() == 1) {
        column(names.get(0)).unique = true;
      }
    }

    Column column(String name) {
      return columns.get(key(name));
    }

    /** The entity of the table; null when the table has a problem, which the problems get. */
    Entity entity(Map<String, String> names, Entity known, List<String> problems) {
      if (primaryKey.size() != 1) {
        problems.add(
            name
                + (primaryKey.isEmpty()
                    ? ": there is no primary key"
                    : ": a primary key of several columns is not supported"));
        return null;
      }
      Column key = column(primaryKey.get(0));
      Map<String, String> members = members(known);
      Id id = key.id(name, members, problems);
      Column created = column(Entity.CREATED_AT);
      Column updated = column(Entity.UPDATED_AT);
      boolean audit =
          created != null
              && updated != null
              && created.is(FieldType.INSTANT)
              && updated.is(FieldType.INSTANT);
      List<Field> fields = new ArrayList<>();
      List<Relation> relations = new ArrayList<>();
      for (Column column : columns.values()) {
        if (column == key || audit && (column == created || column == updated)) {
          continue;
        }
        String path = name + "." + column.name;
        if (column.references != null) {
          String target = names.get(key(column.references));
          if (target == null) {
            problems.add(
                path + ": there is no table " + column.references + " in the DDL or the service");
          } else {
            relations.add(column.relation(target, members));
          }
        } else {
          Field field = column.field(path, members, problems);
          if (field != null) {
            fields.add(field);
          }
        }
      }
      return new Entity(
          names.get(key(name)),
          name,
          id,
          fields,
          relations,
          audit,
          known == null ? null : known.options());
    }
  }

  /** A column of a table, with what its definition and the keys of the table say of it. */
  private static final class Column {

    final String name;
    final ColumnDefinition definition;
    final SqlType type;
    final boolean notNull;
    final boolean primary;
    boolean identity;
    boolean unique;
    String references;

    Column(ColumnDefinition definition) {
      this.definition = definition;
      name = unquote(definition.getColumnName());
      type = SqlType.of(definition.getColDataType());
      String specs =
          definition.getColumnSpecs() == null
              ? ""
              : LITERAL.matcher(String.join(" ", definition.getColumnSpecs())).replaceAll("''");
      notNull = NOT_NULL.matcher(specs).find();
      primary = PRIMARY_KEY.matcher(specs).find();
      identity = IDENTITY.matcher(specs).find();
      unique = UNIQUE.matcher(specs).find();
      Matcher target = REFERENCES.matcher(specs);
      if (target.find()) {
        String[] parts = target.group(1).split("\\.");
        references = unquote(parts[parts.length - 1]);
      }
    }

    /** Whether the column holds values of the field type. */
    boolean is(FieldType fieldType) {
      return type != null && type.type() == fieldType;
    }

    /**
     * The identifier that the column holds: a Long that the database assigns, by identity or by a
     * sequence, or a UUID.
     */
    Id id(String table, Map<String, String> members, List<String> problems) {
      String field = member(members, name);
      if (is(FieldType.UUID)) {
        return new Id(field, IdType.UUID, IdStrategy.UUID);
      }
      if (!is(FieldType.INTEGER) && !is(FieldType.LONG)) {
        problems.add(
            table
                + "."
                + name
                + ": the primary key is "
                + SqlType.text(definition.getColDataType())
                + ", and only an integer or a UUID is supported");
        return null;
      }
      return new Id(
          field,
          IdType.LONG,
          identity || type.serial() ? IdStrategy.IDENTITY : IdStrategy.SEQUENCE);
    }

    Field field(String path, Map<String, String> members, List<String> problems) {
      if (type == null) {
        problems.add(
            path + ": the type " + SqlType.text(definition.getColDataType()) + " is not supported");
        return null;
      }
      String field = member(members, name);
      return new Field(
          field,
          name,
          type.type(),
          type.length(),
          type.precision(),
          type.scale(),
          !notNull,
          unique);
    }

    /**
     * The relation that the column holds: one to one when the column is unique. Its field is named
     * after the column without {@code _ID}.
     */
    Relation relation(String target, Map<String, String> members) {
      String field = member(members, ID_SUFFIX.matcher(name).replaceFirst("$1"));
      return new Relation(
          unique ? RelationKind.ONE_TO_ONE : RelationKind.MANY_TO_ONE,
          target,
          field,
          name,
          !notNull);
    }

    /**
     * The name of the member of the column: the one the spec gives it, or one derived from the
     * name.
     */
    private String member(Map<String, String> members, String derivedFrom) {
      return members.getOrDefault(key(name), Names.member(Names.type(derivedFrom)));
    }
  }
}
