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

import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Relation;
import me.yakar.tib.spec.RelationKind;

/** A relation as the templates see it, with its target. */
public record RelationModel(Relation relation, EntityModel target) {

  /** The field that holds the target: {@code depot}. */
  public String field() {
    return relation.field();
  }

  /** The field with its first letter in upper case, for the accessors. */
  public String property() {
    return Names.property(relation.field());
  }

  /** The field of the DTO that holds the identifier of the target: {@code depotId}. */
  public String idField() {
    return relation.field() + Names.property(target.idField());
  }

  /** The column that holds the identifier of the target: {@code DEPOT_ID}. */
  public String column() {
    return relation.column();
  }

  boolean nullable() {
    return relation.nullable();
  }

  boolean oneToOne() {
    return relation.kind() == RelationKind.ONE_TO_ONE;
  }

  /** The JPA annotation of the relation's kind. */
  public String annotation() {
    return oneToOne() ? "OneToOne" : "ManyToOne";
  }

  /** The JPA annotation of the relation: {@code @ManyToOne(fetch = FetchType.LAZY)}. */
  public String mapping() {
    return "@"
        + annotation()
        + "(fetch = FetchType.LAZY"
        + (relation.nullable() ? "" : ", optional = false")
        + ")";
  }

  /** The JPA annotation of the relation's column. */
  public String joinColumn() {
    return "@JoinColumn(name = \""
        + relation.column()
        + "\""
        + (relation.nullable() ? "" : ", nullable = false")
        + (oneToOne() ? ", unique = true" : "")
        + ")";
  }

  /** The validation annotation of the DTO's component, in front of its type. */
  String validation() {
    return relation.nullable() ? "" : "@NotNull ";
  }

  /** The definition of the column in SQL, with its foreign key. */
  String sql() {
    return relation.column()
        + " "
        + FieldModel.sqlType(target.entity().id().type())
        + (relation.nullable() ? "" : " NOT NULL")
        + (oneToOne() ? " UNIQUE" : "")
        + " REFERENCES "
        + target.table()
        + " ("
        + target.idColumn()
        + ")";
  }
}
