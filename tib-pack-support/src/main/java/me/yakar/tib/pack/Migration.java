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
package me.yakar.tib.pack;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.yakar.tib.tree.ProjectFiles;

/**
 * The Flyway migration that creates the table of an entity, whatever the stack that reads it: its
 * path among the migrations of the service, and its SQL. A migration lasts: a database may have run
 * it, so it stays when the entity leaves the spec.
 */
public final class Migration {

  /** The directory of the migrations, where Flyway looks for them by default. */
  public static final String DIRECTORY = "src/main/resources/db/migration";

  /**
   * A versioned migration, at any depth: the major part of its version, as in V2__, V2.1__, V2_1__.
   */
  private static final Pattern VERSION = Pattern.compile("/V(\\d+)(?:[._]\\d+)*__");

  private Migration() {}

  /** Stages the migration that creates the table of the entity, as a file that lasts. */
  public static void write(ProjectFiles files, EntityModel entity) {
    String path = path(files, entity);
    files.lasting(path, Templates.comment(path, Header.MIGRATION.text()) + "\n" + sql(entity));
  }

  /**
   * The migration that creates the table: the one tib wrote before, there or not, so that a
   * migration the developer removed or renamed stays so; else the one there; else the next major
   * version after the versioned migrations there.
   */
  static String path(ProjectFiles files, EntityModel entity) {
    String suffix = "__create_" + entity.table().toLowerCase(Locale.ROOT) + ".sql";
    for (String migration : files.recorded(DIRECTORY)) {
      if (migration.endsWith(suffix)) {
        return migration;
      }
    }
    long latest = 0;
    for (String migration : files.list(DIRECTORY)) {
      if (migration.endsWith(suffix)) {
        return migration;
      }
      Matcher version = VERSION.matcher(migration);
      if (version.find()) {
        latest = Math.max(latest, Long.parseLong(version.group(1)));
      }
    }
    return DIRECTORY + "/V" + (latest + 1) + suffix;
  }

  /**
   * The SQL of the migration: the sequence of a sequence identifier, the table with its columns,
   * and an index on the column of every many-to-one relation.
   */
  static String sql(EntityModel entity) {
    StringBuilder sql = new StringBuilder();
    if (entity.idSequence()) {
      sql.append("CREATE SEQUENCE ")
          .append(entity.sequence())
          .append(" START WITH 1 INCREMENT BY ")
          .append(entity.sequenceIncrement())
          .append(";\n\n");
    }
    sql.append("CREATE TABLE ").append(entity.table()).append(" (\n");
    sql.append(String.join(",\n", entity.columns().stream().map(column -> "  " + column).toList()));
    sql.append("\n);\n");
    List<RelationModel> indexed = entity.indexed();
    if (!indexed.isEmpty()) {
      sql.append('\n');
      for (RelationModel relation : indexed) {
        sql.append("CREATE INDEX ")
            .append(entity.index(relation))
            .append(" ON ")
            .append(entity.table())
            .append(" (")
            .append(relation.column())
            .append(");\n");
      }
    }
    return sql.toString();
  }
}
