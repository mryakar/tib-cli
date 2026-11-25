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

import static me.yakar.tib.ddl.DdlReader.key;
import static me.yakar.tib.ddl.DdlReader.unquote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.Field;
import me.yakar.tib.spec.FieldType;
import me.yakar.tib.spec.Id;
import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Relation;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

/** A table that the DDL creates, with the keys that it gives the table. */
final class Table {

  /** A column that an {@code ALTER TABLE} makes an identity, as {@code ID ADD GENERATED ... }. */
  private static final Pattern ADD_IDENTITY =
      Pattern.compile(
          "^\\S+\\s+ADD\\s+GENERATED\\s+(?:ALWAYS|BY\\s+DEFAULT)\\s+AS\\s+IDENTITY$",
          Pattern.CASE_INSENSITIVE);

  /** The owner or the replica identity that an {@code ALTER TABLE} gives the table. */
  private static final Pattern OWNER_OR_REPLICA_IDENTITY =
      Pattern.compile("^(?:OWNER\\s+TO|REPLICA\\s+IDENTITY)\\b", Pattern.CASE_INSENSITIVE);

  /** A column whose statistics an {@code ALTER TABLE} sets, as {@code ID SET STATISTICS 100}. */
  private static final Pattern SET_STATISTICS =
      Pattern.compile("\\sSET\\s+STATISTICS\\s+\\S+$", Pattern.CASE_INSENSITIVE);

  final String name;
  private final Map<String, Column> columns = new LinkedHashMap<>();
  private List<String> primaryKey = List.of();

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
   * and skips the defaults that it sets and what says nothing of the entity; any other change is a
   * problem.
   */
  void alter(Alter alter, List<String> problems) {
    for (AlterExpression expression : alter.getAlterExpressions()) {
      if (expression.getOperation() == AlterOperation.ADD && expression.getIndex() != null) {
        take(expression.getIndex(), problems);
      } else if (skipped(expression)) {
        // The change says nothing of the entity.
      } else if (expression.getColumnSetDefaultList() != null) {
        for (AlterExpression.ColumnSetDefault set : expression.getColumnSetDefaultList()) {
          identify(set.getColumnName(), Column.nextval(set.toString()), problems);
        }
      } else if (identities(expression)) {
        for (AlterExpression.ColumnDataType identity : expression.getColDataTypeList()) {
          identify(identity.getColumnName(), true, problems);
        }
      } else {
        problems.add(
            name
                + ": ALTER TABLE may only add keys, make a column an identity, or set a default"
                + " or the owner");
      }
    }
  }

  /** Makes the column an identity, when it becomes one; a column that is not there is a problem. */
  private void identify(String name, boolean identity, List<String> problems) {
    String unquoted = unquote(name);
    Column column = column(unquoted);
    if (column == null) {
      problems.add(this.name + ": there is no column " + unquoted);
    } else {
      column.identity |= identity;
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
  private void take(Index index, List<String> problems) {
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

  private Column column(String name) {
    return columns.get(key(name));
  }

  /** Whether a Java name comes of the SQL name; one of underscores alone is a problem. */
  private static boolean named(String sqlName, String path, List<String> problems) {
    if (Names.type(sqlName).isEmpty()) {
      problems.add(path + ": a name of underscores alone is not supported");
      return false;
    }
    return true;
  }

  /** The entity of the table; null when the table has a problem, which the problems get. */
  Entity entity(Map<String, String> names, Entity known, List<String> problems) {
    boolean named = named(name, name, problems);
    for (Column column : columns.values()) {
      named &= named(column.name, name + "." + column.name, problems);
    }
    if (!named) {
      return null;
    }
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

  /**
   * Whether the change says nothing of the entity: the owner of the table, its row level security,
   * its replica identity, or the statistics of a column.
   */
  static boolean skipped(AlterExpression expression) {
    return switch (expression.getOperation()) {
      case ENABLE_ROW_LEVEL_SECURITY,
          DISABLE_ROW_LEVEL_SECURITY,
          FORCE_ROW_LEVEL_SECURITY,
          NO_FORCE_ROW_LEVEL_SECURITY ->
          true;
      case UNSPECIFIC ->
          OWNER_OR_REPLICA_IDENTITY
              .matcher(String.valueOf(expression.getOptionalSpecifier()))
              .find();
      case ALTER ->
          expression.getColDataTypeList() != null
              && expression.getColDataTypeList().stream()
                  .allMatch(column -> SET_STATISTICS.matcher(column.toString()).find());
      default -> false;
    };
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
}
