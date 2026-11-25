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
package me.yakar.tib.pack.spring;

import java.util.List;
import java.util.stream.Collectors;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.FieldModel;
import me.yakar.tib.pack.RelationModel;

/**
 * The persistence of an entity as the templates of the Spring pack see it: the class that JPA maps
 * to its table, with the annotations of its members, and its repository of Spring Data.
 */
public final class JpaModel {

  /**
   * How many identifiers Hibernate takes from a sequence at a time: the increment of the sequence,
   * which Hibernate checks when it starts.
   */
  static final int ALLOCATION_SIZE = 50;

  private final EntityModel entity;

  public JpaModel(EntityModel entity) {
    this.entity = entity;
  }

  /**
   * The class that JPA maps to the table: the entity, or its {@code Entity} in the hexagonal
   * architecture, whose domain knows no JPA.
   */
  public String entityClass() {
    return entity.hexagonal() ? entity.name() + "Entity" : entity.name();
  }

  /**
   * The repository of the entity: {@code VehicleRepository}, or {@code VehicleJpaRepository} in the
   * hexagonal architecture.
   */
  public String repository() {
    return repository(entity);
  }

  /** The repository of an entity of the service, named as {@link #repository()} names it. */
  public String repository(EntityModel model) {
    return model.name() + (model.hexagonal() ? "JpaRepository" : "Repository");
  }

  /**
   * The parameters of the constructor of the mapper between the entity and its DTO: the
   * repositories of the targets of the relations.
   */
  public String repositoryParameters() {
    return entity.targets().stream()
        .map(target -> target.name() + "Repository " + target.plural())
        .collect(Collectors.joining(", "));
  }

  /** The annotations of the identifier, before the one of its column. */
  public List<String> idAnnotations() {
    return switch (entity.idStrategy()) {
      case IDENTITY -> List.of("@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)");
      case SEQUENCE ->
          List.of(
              "@Id",
              "@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = \""
                  + entity.variable()
                  + "\")",
              "@SequenceGenerator(name = \""
                  + entity.variable()
                  + "\", sequenceName = \""
                  + entity.sequence()
                  + "\", allocationSize = "
                  + ALLOCATION_SIZE
                  + ")");
      case UUID -> List.of("@Id", "@GeneratedValue(strategy = GenerationType.UUID)");
    };
  }

  /** The annotation of the column of a field. */
  public String column(FieldModel field) {
    StringBuilder column = new StringBuilder("@Column(name = \"" + field.column() + "\"");
    if (!field.nullable()) {
      column.append(", nullable = false");
    }
    if (field.unique()) {
      column.append(", unique = true");
    }
    if (field.length() != null) {
      column.append(", length = ").append(field.length());
    }
    if (field.precision() != null) {
      column.append(", precision = ").append(field.precision());
    }
    if (field.scale() != null) {
      column.append(", scale = ").append(field.scale());
    }
    return column.append(")").toString();
  }

  /**
   * The annotation of the column that keeps the identifier of the target of a relation, for an
   * entity that refers to another by its identifier.
   */
  public String column(RelationModel relation) {
    return "@Column(" + columnAttributes(relation) + ")";
  }

  /** The annotation of the kind of a relation, by its simple name. */
  public String annotation(RelationModel relation) {
    return relation.oneToOne() ? "OneToOne" : "ManyToOne";
  }

  /** The annotation of a relation: {@code @ManyToOne(fetch = FetchType.LAZY)}. */
  public String mapping(RelationModel relation) {
    return "@"
        + annotation(relation)
        + "(fetch = FetchType.LAZY"
        + (relation.nullable() ? "" : ", optional = false")
        + ")";
  }

  /** The annotation of the column of a relation. */
  public String joinColumn(RelationModel relation) {
    return "@JoinColumn(" + columnAttributes(relation) + ")";
  }

  /** The name of the column of a relation, and its rules. */
  private static String columnAttributes(RelationModel relation) {
    return "name = \""
        + relation.column()
        + "\""
        + (relation.nullable() ? "" : ", nullable = false")
        + (relation.oneToOne() ? ", unique = true" : "");
  }
}
