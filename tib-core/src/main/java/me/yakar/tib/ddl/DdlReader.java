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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import me.yakar.tib.spec.SpecReader;
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
 * TABLE} may add keys to the tables; other statements, such as those that create sequences, indexes
 * or comments, are skipped.
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

  /** The suffix of the column of a relation, after the name of the relation. */
  private static final Pattern ID_SUFFIX = Pattern.compile("(?i)(.)_ID$");

  /** The columns that an audited entity's table has, in the default pack. */
  private static final String CREATED_AT = "CREATED_AT";

  private static final String UPDATED_AT = "UPDATED_AT";

  private DdlReader() {}

  /** Reads the DDL file into the spec of the service, as {@link #read(String, Spec)} does. */
  public static Spec read(Path file, Spec service) {
    try {
      return read(Files.readString(file), service);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Reads DDL into the spec of the service. The entity of a table takes the place of the entity of
   * the same table in the spec, and keeps its name, its options and the names of the fields and
   * relations of the same columns; the entities of the other tables come after those of the spec.
   */
  public static Spec read(String ddl, Spec service) {
    List<String> problems = new ArrayList<>();
    Map<String, Table> tables = tables(ddl, problems);
    if (tables.isEmpty() && problems.isEmpty()) {
      problems.add("there is no CREATE TABLE");
    }
    Map<String, Entity> known = new LinkedHashMap<>();
    for (Entity entity : service.entities()) {
      known.putIfAbsent(key(entity.table()), entity);
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
    return SpecReader.check(
        new Spec(service.tib(), service.service(), service.group(), service.pack(), merged));
  }

  /** The tables that the DDL creates, by their keys, with the keys that it adds to them. */
  private static Map<String, Table> tables(String ddl, List<String> problems) {
    List<Statement> statements;
    try {
      statements = CCJSqlParserUtil.parseStatements(ddl);
    } catch (JSQLParserException e) {
      problems.add("it is not SQL that tib reads: " + reason(e));
      return Map.of();
    }
    Map<String, Table> tables = new LinkedHashMap<>();
    for (Statement statement : statements) {
      if (statement instanceof CreateTable create) {
        Table table = new Table(create, problems);
        if (tables.putIfAbsent(key(table.name), table) != null) {
          problems.add(table.name + ": the table is created twice");
        }
      } else if (statement instanceof Alter alter) {
        String name = unquote(alter.getTable().getName());
        Table table = tables.get(key(name));
        if (table == null) {
          problems.add(name + ": ALTER TABLE comes before the table is created");
        } else {
          table.alter(alter, problems);
        }
      }
    }
    return tables;
  }

  /** Where the parser stopped, as in {@code Encountered: "(", at line 2, column 10}. */
  private static String reason(JSQLParserException e) {
    return e.getMessage()
        .lines()
        .findFirst()
        .orElse("")
        .replaceFirst("^.*?(Encountered)", "$1")
        .replaceFirst(", in lexical state.*$", "");
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

    /** Takes the keys that the statement adds to the table; any other change is a problem. */
    void alter(Alter alter, List<String> problems) {
      for (AlterExpression expression : alter.getAlterExpressions()) {
        if (expression.getOperation() == AlterOperation.ADD && expression.getIndex() != null) {
          take(expression.getIndex(), problems);
        } else {
          problems.add(name + ": ALTER TABLE may only add keys");
        }
      }
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
      Id id = key.id(name, known, problems);
      Column created = column(CREATED_AT);
      Column updated = column(UPDATED_AT);
      boolean audit = created != null && updated != null && created.instant() && updated.instant();
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
            relations.add(column.relation(target, known));
          }
        } else {
          Field field = column.field(path, known, problems);
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
    final boolean identity;
    boolean unique;
    String references;

    Column(ColumnDefinition definition) {
      this.definition = definition;
      name = unquote(definition.getColumnName());
      type = SqlType.of(definition.getColDataType());
      String specs =
          definition.getColumnSpecs() == null ? "" : String.join(" ", definition.getColumnSpecs());
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

    /** Whether the column holds an instant: the columns of an audited entity do. */
    boolean instant() {
      return type != null && type.type() == FieldType.INSTANT;
    }

    /**
     * The identifier that the column holds: a Long that the database assigns, by identity or by a
     * sequence, or a UUID.
     */
    Id id(String table, Entity known, List<String> problems) {
      String field =
          known != null && Names.sql(known.id().field()).equalsIgnoreCase(name)
              ? known.id().field()
              : Names.member(Names.type(name));
      if (type != null && type.type() == FieldType.UUID) {
        return new Id(field, IdType.UUID, IdStrategy.UUID);
      }
      if (type == null || type.type() != FieldType.INTEGER && type.type() != FieldType.LONG) {
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

    Field field(String path, Entity known, List<String> problems) {
      if (type == null) {
        problems.add(
            path + ": the type " + SqlType.text(definition.getColDataType()) + " is not supported");
        return null;
      }
      String field = Names.member(Names.type(name));
      if (known != null) {
        for (Field same : known.fields()) {
          if (same.column().equalsIgnoreCase(name)) {
            field = same.name();
          }
        }
      }
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
    Relation relation(String target, Entity known) {
      String field = Names.member(Names.type(ID_SUFFIX.matcher(name).replaceFirst("$1")));
      if (known != null) {
        for (Relation same : known.relations()) {
          if (same.column().equalsIgnoreCase(name)) {
            field = same.field();
          }
        }
      }
      return new Relation(
          unique ? RelationKind.ONE_TO_ONE : RelationKind.MANY_TO_ONE,
          target,
          field,
          name,
          !notNull);
    }
  }
}
