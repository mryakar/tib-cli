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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.yakar.tib.spec.Field;
import me.yakar.tib.spec.FieldType;
import me.yakar.tib.spec.Id;
import me.yakar.tib.spec.IdStrategy;
import me.yakar.tib.spec.IdType;
import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Relation;
import me.yakar.tib.spec.RelationKind;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

/** A column of a table, with what its definition and the keys of the table say of it. */
final class Column {

  private static final Pattern NOT_NULL =
      Pattern.compile("\\bNOT\\s+NULL\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern PRIMARY_KEY =
      Pattern.compile("\\bPRIMARY\\s+KEY\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern UNIQUE = Pattern.compile("\\bUNIQUE\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern IDENTITY =
      Pattern.compile("\\b(IDENTITY|AUTO_INCREMENT)\\b", Pattern.CASE_INSENSITIVE);

  /** A default of the next value of a sequence, which the database assigns as for an identity. */
  private static final Pattern NEXTVAL =
      Pattern.compile("\\bNEXTVAL\\s*\\(", Pattern.CASE_INSENSITIVE);

  private static final Pattern REFERENCES =
      Pattern.compile("\\bREFERENCES\\s+([^\\s(]+)", Pattern.CASE_INSENSITIVE);

  /** A string literal, which the keywords above are never found in. */
  private static final Pattern LITERAL = Pattern.compile("'(?:[^']|'')*'");

  /** The suffix of the column of a relation, after the name of the relation. */
  private static final Pattern ID_SUFFIX = Pattern.compile("(?i)(?<=[^_])_ID$");

  final String name;
  private final ColumnDefinition definition;
  private final SqlType type;
  private final boolean notNull;
  final boolean primary;
  boolean identity;
  boolean unique;
  String references;

  Column(ColumnDefinition definition) {
    this.definition = definition;
    name = unquote(definition.getColumnName());
    type = SqlType.of(definition.getColDataType());
    List<String> specs = Objects.requireNonNullElse(definition.getColumnSpecs(), List.of());
    // An expression in parentheses, as that of a check, says nothing of the column.
    String constraints =
        withoutLiterals(
            specs.stream().filter(spec -> !spec.startsWith("(")).collect(Collectors.joining(" ")));
    notNull = NOT_NULL.matcher(constraints).find();
    primary = PRIMARY_KEY.matcher(constraints).find();
    identity = IDENTITY.matcher(constraints).find() || nextval(String.join(" ", specs));
    unique = UNIQUE.matcher(constraints).find();
    Matcher target = REFERENCES.matcher(constraints);
    if (target.find()) {
      String[] parts = target.group(1).split("\\.");
      references = unquote(parts[parts.length - 1]);
    }
  }

  /** The SQL with its string literals emptied. */
  private static String withoutLiterals(String sql) {
    return LITERAL.matcher(sql).replaceAll("''");
  }

  /** Whether the SQL has the next value of a sequence, outside its string literals. */
  static boolean nextval(String sql) {
    return NEXTVAL.matcher(withoutLiterals(sql)).find();
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
      return new Id(field, name, IdType.UUID, IdStrategy.UUID);
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
        name,
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
        field, name, type.type(), type.length(), type.precision(), type.scale(), !notNull, unique);
  }

  /**
   * The relation that the column holds: one to one when the column is unique. Its field is named
   * after the column without {@code _ID}.
   */
  Relation relation(String target, Map<String, String> members) {
    String field = member(members, ID_SUFFIX.matcher(name).replaceFirst(""));
    return new Relation(
        unique ? RelationKind.ONE_TO_ONE : RelationKind.MANY_TO_ONE, target, field, name, !notNull);
  }

  /**
   * The name of the member of the column: the one the spec gives it, or one derived from the name.
   */
  private String member(Map<String, String> members, String derivedFrom) {
    return members.getOrDefault(key(name), Names.member(Names.type(derivedFrom)));
  }
}
