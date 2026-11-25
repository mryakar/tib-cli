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
package me.yakar.tib.pack.plain;

import java.util.ArrayList;
import java.util.List;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.FieldModel;
import me.yakar.tib.pack.RelationModel;
import me.yakar.tib.spec.FieldType;

/**
 * The repository of an entity as the templates of the plain pack see it: the SQL of its statements
 * on the table of the migration, the values that they bind, and how a row is read. The audit times
 * are set by the SQL, and a relation is the identifier of its target.
 */
public final class RepositoryModel {

  /** The time at which the statement runs, which the SQL gives the audit times. */
  private static final String NOW = "CURRENT_TIMESTAMP";

  /**
   * A value of the entity that the repository sets from a row: the accessor of the entity, after
   * {@code set}, and the Java that reads the value from the row.
   */
  public record Read(String property, String value) {}

  private final EntityModel entity;

  public RepositoryModel(EntityModel entity) {
    this.entity = entity;
  }

  /** The columns of the table, in the order of the spec, as a select lists them. */
  public String columns() {
    List<String> columns = new ArrayList<>();
    columns.add(entity.idColumn());
    for (FieldModel field : entity.fields()) {
      columns.add(field.column());
    }
    for (RelationModel relation : entity.relations()) {
      columns.add(relation.column());
    }
    if (entity.audited()) {
      columns.add(entity.createdAt());
      columns.add(entity.updatedAt());
    }
    return String.join(", ", columns);
  }

  /**
   * The statement that inserts a row and gives back what the database gave it: the identifier from
   * its identity column or its sequence, or a UUID that the repository bound; the audit times.
   */
  public String insert() {
    List<String> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();
    // An identity column gives the identifier itself.
    if (entity.idSequence()) {
      columns.add(entity.idColumn());
      values.add("nextval('" + entity.sequence() + "')");
    } else if (entity.uuidId()) {
      columns.add(entity.idColumn());
      values.add("?");
    }
    for (String column : valueColumns()) {
      columns.add(column);
      values.add("?");
    }
    if (entity.audited()) {
      columns.add(entity.createdAt());
      values.add(NOW);
      columns.add(entity.updatedAt());
      values.add(NOW);
    }
    String insert =
        "INSERT INTO "
            + entity.table()
            + (columns.isEmpty()
                ? " DEFAULT VALUES"
                : " ("
                    + String.join(", ", columns)
                    + ") VALUES ("
                    + String.join(", ", values)
                    + ")");
    List<String> returned = insertReturned();
    return returned.isEmpty() ? insert : insert + " RETURNING " + String.join(", ", returned);
  }

  /** The values that the insert binds, as Java on the entity in its variable. */
  public List<String> insertParameters() {
    List<String> parameters = new ArrayList<>();
    if (entity.uuidId()) {
      parameters.add(entity.variable() + ".getId()");
    }
    parameters.addAll(valueParameters());
    return parameters;
  }

  /** What the insert gives back, in the order of {@link #insert}. */
  public List<Read> insertReads() {
    List<Read> reads = new ArrayList<>();
    if (!entity.uuidId()) {
      reads.add(id());
    }
    reads.addAll(audit(true));
    return reads;
  }

  /**
   * The statement that updates the row of an entity, all but its identifier and its creation time,
   * and gives back the time of the change of an audited entity. An entity without other values sets
   * its identifier to itself, which tells whether the row is there.
   */
  public String update() {
    List<String> sets = new ArrayList<>();
    for (String column : valueColumns()) {
      sets.add(column + " = ?");
    }
    if (entity.audited()) {
      sets.add(entity.updatedAt() + " = " + NOW);
    }
    if (sets.isEmpty()) {
      sets.add(entity.idColumn() + " = " + entity.idColumn());
    }
    return "UPDATE "
        + entity.table()
        + " SET "
        + String.join(", ", sets)
        + " WHERE "
        + entity.idColumn()
        + " = ?"
        + (entity.audited() ? " RETURNING " + entity.updatedAt() : "");
  }

  /** The values that the update binds, its identifier last, as Java on the entity. */
  public List<String> updateParameters() {
    List<String> parameters = valueParameters();
    parameters.add(entity.variable() + ".getId()");
    return parameters;
  }

  /** What the update gives back. */
  public List<Read> updateReads() {
    return audit(false);
  }

  /** How every value of the entity is read from a row of {@link #columns}. */
  public List<Read> reads() {
    List<Read> reads = new ArrayList<>();
    reads.add(id());
    for (FieldModel field : entity.fields()) {
      String column = field.column();
      reads.add(
          new Read(
              field.property(),
              instant(field)
                  ? "instant(" + get(column, "OffsetDateTime") + ")"
                  : get(column, field.javaType())));
    }
    for (RelationModel relation : entity.relations()) {
      reads.add(
          new Read(relation.idProperty(), get(relation.column(), relation.target().idType())));
    }
    reads.addAll(audit(true));
    return reads;
  }

  /**
   * Whether a field is an {@code Instant}, which the repository binds and reads as the {@code
   * OffsetDateTime} of the driver.
   */
  public boolean instants() {
    return entity.fields().stream().anyMatch(RepositoryModel::instant);
  }

  /** The columns of the fields and of the relations, which the statements bind. */
  private List<String> valueColumns() {
    List<String> columns = new ArrayList<>();
    entity.fields().forEach(field -> columns.add(field.column()));
    entity.relations().forEach(relation -> columns.add(relation.column()));
    return columns;
  }

  /** The values of the fields and of the relations, as Java on the entity. */
  private List<String> valueParameters() {
    String variable = entity.variable();
    List<String> parameters = new ArrayList<>();
    for (FieldModel field : entity.fields()) {
      String value = variable + ".get" + field.property() + "()";
      parameters.add(instant(field) ? "offset(" + value + ")" : value);
    }
    for (RelationModel relation : entity.relations()) {
      parameters.add(variable + ".get" + relation.idProperty() + "()");
    }
    return parameters;
  }

  /** The columns that the insert gives back. */
  private List<String> insertReturned() {
    List<String> columns = new ArrayList<>();
    if (!entity.uuidId()) {
      columns.add(entity.idColumn());
    }
    if (entity.audited()) {
      columns.add(entity.createdAt());
      columns.add(entity.updatedAt());
    }
    return columns;
  }

  private Read id() {
    return new Read("Id", get(entity.idColumn(), entity.idType()));
  }

  /** The audit times of an audited entity, the creation time with the other or not. */
  private List<Read> audit(boolean created) {
    List<Read> reads = new ArrayList<>();
    if (entity.audited()) {
      if (created) {
        reads.add(new Read("CreatedAt", time(entity.createdAt())));
      }
      reads.add(new Read("UpdatedAt", time(entity.updatedAt())));
    }
    return reads;
  }

  /** An audit time, which is never null. */
  private static String time(String column) {
    return get(column, "OffsetDateTime") + ".toInstant()";
  }

  private static String get(String column, String type) {
    return "rows.getObject(\"" + column + "\", " + type + ".class)";
  }

  private static boolean instant(FieldModel field) {
    return field.type() == FieldType.INSTANT;
  }
}
