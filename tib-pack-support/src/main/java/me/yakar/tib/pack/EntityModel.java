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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.IdStrategy;
import me.yakar.tib.spec.IdType;
import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Relation;

/**
 * An entity as the templates see it: its names, its fields and relations, and what the generated
 * code declares. The templates lay the code out.
 */
public final class EntityModel {

  /**
   * How many identifiers Hibernate takes from a sequence at a time: the increment of the sequence,
   * which Hibernate checks when it starts.
   */
  private static final int ALLOCATION_SIZE = 50;

  private final Project project;
  private final Entity entity;
  private final boolean byIdentifier;
  private final List<FieldModel> fields;
  private final List<RelationModel> relations;
  private final List<EntityModel> targets;

  /**
   * The model of an entity, after the models of the entities it refers to: those are in the map, by
   * name.
   */
  public EntityModel(Project project, Entity entity, Map<String, EntityModel> models) {
    this(project, entity, models, false);
  }

  /**
   * The model of an entity, as {@link #EntityModel(Project, Entity, Map)} makes it.
   *
   * @param byIdentifier whether the entity refers to another by the identifier of its target
   *     whatever the architecture, as in a stack without JPA
   */
  public EntityModel(
      Project project, Entity entity, Map<String, EntityModel> models, boolean byIdentifier) {
    this.project = project;
    this.entity = entity;
    this.byIdentifier = byIdentifier;
    this.fields = entity.fields().stream().map(FieldModel::new).toList();
    List<RelationModel> relations = new ArrayList<>();
    List<EntityModel> targets = new ArrayList<>();
    for (var relation : entity.relations()) {
      EntityModel target =
          relation.target().equals(entity.name()) ? this : models.get(relation.target());
      relations.add(new RelationModel(relation, target));
      if (!targets.contains(target)) {
        targets.add(target);
      }
    }
    this.relations = List.copyOf(relations);
    this.targets = List.copyOf(targets);
  }

  /**
   * The models of the entities of the project, by name, each after the entities it refers to, so
   * that its migration can reference their tables.
   *
   * @param byIdentifier whether every entity refers to another by the identifier of its target
   *     whatever the architecture, as in a stack without JPA
   */
  public static Map<String, EntityModel> models(Project project, boolean byIdentifier) {
    Map<String, EntityModel> models = new LinkedHashMap<>();
    for (Entity entity : ordered(project.spec().entities())) {
      models.put(entity.name(), new EntityModel(project, entity, models, byIdentifier));
    }
    return models;
  }

  /**
   * The entities, each after the entities it refers to; otherwise in the order of the spec. A spec
   * has no cycle of relations, but an entity may refer to itself.
   */
  static List<Entity> ordered(List<Entity> entities) {
    Map<String, Entity> byName = new LinkedHashMap<>();
    entities.forEach(entity -> byName.put(entity.name(), entity));
    Set<String> done = new LinkedHashSet<>();
    byName.keySet().forEach(name -> visit(name, byName, done));
    return done.stream().map(byName::get).toList();
  }

  private static void visit(String name, Map<String, Entity> entities, Set<String> done) {
    if (done.contains(name)) {
      return;
    }
    for (Relation relation : entities.get(name).relations()) {
      if (!relation.target().equals(name)) {
        visit(relation.target(), entities, done);
      }
    }
    done.add(name);
  }

  /** The base package of the service. */
  public String basePackage() {
    return project.basePackage();
  }

  public String name() {
    return entity.name();
  }

  /** The name as a variable: {@code vehicle}. */
  public String variable() {
    return Names.member(entity.name());
  }

  /** The article of the name: {@code A} or {@code An}. */
  public String article() {
    return "AEIOU".indexOf(entity.name().charAt(0)) < 0 ? "A" : "An";
  }

  /** The name of the collection: {@code vehicles}. */
  public String plural() {
    return Names.plural(variable());
  }

  /** The path of the API: {@code /api/vehicles}. */
  public String path() {
    return "/api/" + plural();
  }

  public String table() {
    return entity.table();
  }

  /** The package of the entity's classes: one per entity, under the base package. */
  public String packageName() {
    return project.basePackage() + "." + entity.packageSegment();
  }

  /**
   * Whether the service is of the hexagonal architecture: the domain of the entity in plain Java,
   * its application with its ports, and its adapters.
   */
  public boolean hexagonal() {
    return project.architecture() == Architecture.HEXAGONAL;
  }

  /**
   * Whether the entity refers to another by the identifier of its target, a column of its own, and
   * not by a relation of JPA: in DDD and in the hexagonal architecture, and in a stack without JPA.
   */
  public boolean refersById() {
    return byIdentifier || project.architecture() != Architecture.ANEMIC;
  }

  /**
   * The package of the entity, and of its repository but in the hexagonal architecture: its domain,
   * or the package of the entity.
   */
  public String domainPackage() {
    return layer("domain", "domain");
  }

  /** The package of the service: its application, or the package of the entity. */
  public String applicationPackage() {
    return layer("application", "application");
  }

  /**
   * The package of the controller, the DTO and the mapper: its web, its inbound adapter of the web,
   * or the package of the entity.
   */
  public String webPackage() {
    return layer("web", "adapter.in.web");
  }

  /**
   * The package of the class that maps the table, and of its repository: the outbound adapter of
   * the persistence in the hexagonal architecture, else the package of the entity.
   */
  public String persistencePackage() {
    return hexagonal() ? packageName() + ".adapter.out.persistence" : domainPackage();
  }

  /** The package of the use cases, the inbound port of the hexagonal architecture. */
  public String useCasePackage() {
    return applicationPackage() + ".port.in";
  }

  /** The package of the store, the outbound port of the hexagonal architecture. */
  public String storePackage() {
    return applicationPackage() + ".port.out";
  }

  /** The package of a layer: the one of DDD, the one of the hexagonal architecture, or none. */
  private String layer(String ddd, String hexagonal) {
    return switch (project.architecture()) {
      case ANEMIC -> packageName();
      case DDD -> packageName() + "." + ddd;
      case HEXAGONAL -> packageName() + "." + hexagonal;
    };
  }

  /**
   * The class that JPA maps to the table: the entity, or its {@code Entity} in the hexagonal
   * architecture, whose domain knows no JPA.
   */
  public String jpaEntity() {
    return hexagonal() ? name() + "Entity" : name();
  }

  /**
   * The repository of the JPA entity: {@code VehicleRepository}, or {@code VehicleJpaRepository}.
   */
  public String repository() {
    return name() + (hexagonal() ? "JpaRepository" : "Repository");
  }

  /**
   * The mapper between the entity and its DTO: {@code VehicleMapper}, or {@code VehicleWebMapper}.
   */
  public String webMapper() {
    return name() + (hexagonal() ? "WebMapper" : "Mapper");
  }

  public String idField() {
    return entity.id().field();
  }

  public String idColumn() {
    return entity.id().column();
  }

  /** The Java type of the identifier: {@code Long} or {@code UUID}. */
  public String idType() {
    return entity.id().type().toString();
  }

  /** The SQL type of the identifier, and of a column that refers to it. */
  String idSqlType() {
    return uuidId() ? "UUID" : "BIGINT";
  }

  /** Whether the identifier is a UUID. */
  public boolean uuidId() {
    return entity.id().type() == IdType.UUID;
  }

  public boolean idSequence() {
    return entity.id().strategy() == IdStrategy.SEQUENCE;
  }

  /** The database sequence of a sequence identifier. */
  public String sequence() {
    return entity.table() + inCaseOfTable("_SEQ");
  }

  /** The increment of the sequence: the allocation size of its generator. */
  public int sequenceIncrement() {
    return ALLOCATION_SIZE;
  }

  public boolean audited() {
    return entity.audit();
  }

  /** The column of the time an audited entity was created. */
  public String createdAt() {
    return inCaseOfTable(Entity.CREATED_AT);
  }

  /** The column of the time an audited entity was last changed. */
  public String updatedAt() {
    return inCaseOfTable(Entity.UPDATED_AT);
  }

  /**
   * A name, in upper case, that tib adds to the table, in the case of the table: in lower case when
   * the table has no upper-case letter.
   */
  private String inCaseOfTable(String name) {
    return entity.table().chars().anyMatch(Character::isUpperCase)
        ? name
        : name.toLowerCase(Locale.ROOT);
  }

  /** Whether the list of the entities comes in pages. */
  public boolean paginated() {
    return entity.options().pagination();
  }

  /** Whether the entity has a DTO, and so do its samples. */
  public boolean dto() {
    return entity.options().dto();
  }

  /** Whether the service and the controller are generated: they need the DTO and the mapper. */
  public boolean layer() {
    return entity.options().dto() && entity.options().mapper();
  }

  public List<FieldModel> fields() {
    return fields;
  }

  public List<RelationModel> relations() {
    return relations;
  }

  /**
   * The entities that the relations refer to, this one included, once each, in the order of the
   * relations.
   */
  public List<EntityModel> targets() {
    return targets;
  }

  /** The entities that the relations refer to, but for this one: their classes are imported. */
  public List<EntityModel> otherTargets() {
    return targets.stream().filter(target -> target != this).toList();
  }

  /**
   * The relations that are not nullable, but for one to itself, which has to be: a sample of the
   * entity sets them to the samples of their targets.
   */
  public List<RelationModel> requiredRelations() {
    return relations.stream()
        .filter(relation -> !relation.nullable() && relation.target() != this)
        .toList();
  }

  // The generated base class.

  /** The annotations of the identifier, before its column. */
  public List<String> idAnnotations() {
    return switch (entity.id().strategy()) {
      case IDENTITY -> List.of("@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)");
      case SEQUENCE ->
          List.of(
              "@Id",
              "@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = \""
                  + variable()
                  + "\")",
              "@SequenceGenerator(name = \""
                  + variable()
                  + "\", sequenceName = \""
                  + sequence()
                  + "\", allocationSize = "
                  + ALLOCATION_SIZE
                  + ")");
      case UUID -> List.of("@Id", "@GeneratedValue(strategy = GenerationType.UUID)");
    };
  }

  // The DTO.

  /** The components of the record: the type and the name of each, after its validation. */
  public String dtoComponents() {
    List<String> components = new ArrayList<>();
    components.add(idType() + " " + idField());
    for (FieldModel field : fields) {
      components.add(field.validation() + field.javaType() + " " + field.name());
    }
    for (RelationModel relation : relations) {
      components.add(relation.validation() + relation.target().idType() + " " + relation.idField());
    }
    if (entity.audit()) {
      components.add("Instant createdAt");
      components.add("Instant updatedAt");
    }
    return String.join(", ", components);
  }

  // The mapper.

  /** The parameters of the mapper's constructor: the repositories of the relations' targets. */
  public String repositoryParameters() {
    return targets.stream()
        .map(target -> target.name() + "Repository " + target.plural())
        .collect(Collectors.joining(", "));
  }

  /**
   * The arguments of the DTO of an entity, in the order of its components; a relation is the
   * identifier of its target, or null: of the entity it refers to, or the one it keeps for DDD and
   * the hexagonal architecture.
   */
  public String dtoArguments() {
    String variable = variable();
    List<String> arguments = new ArrayList<>();
    arguments.add(variable + ".getId()");
    for (FieldModel field : fields) {
      arguments.add(variable + ".get" + field.property() + "()");
    }
    for (RelationModel relation : relations) {
      String target = variable + ".get" + relation.property() + "()";
      arguments.add(
          refersById()
              ? variable + ".get" + relation.idProperty() + "()"
              : target + " == null ? null : " + target + ".getId()");
    }
    if (entity.audit()) {
      arguments.add(variable + ".getCreatedAt()");
      arguments.add(variable + ".getUpdatedAt()");
    }
    return String.join(", ", arguments);
  }

  // The migration.

  /**
   * The relations whose columns get an index: the many-to-one ones. A one-to-one column is unique,
   * which indexes it.
   */
  public List<RelationModel> indexed() {
    return relations.stream().filter(relation -> !relation.oneToOne()).toList();
  }

  /** The index of the column of a relation. */
  public String index(RelationModel relation) {
    return entity.table() + "_" + relation.column() + inCaseOfTable("_IDX");
  }

  /** The definitions of the columns of the table. */
  public List<String> columns() {
    List<String> columns = new ArrayList<>();
    columns.add(idColumn() + " " + idSql() + " PRIMARY KEY");
    for (FieldModel field : fields) {
      columns.add(field.sql());
    }
    for (RelationModel relation : relations) {
      columns.add(relation.sql());
    }
    if (entity.audit()) {
      columns.add(createdAt() + " TIMESTAMP WITH TIME ZONE NOT NULL");
      columns.add(updatedAt() + " TIMESTAMP WITH TIME ZONE NOT NULL");
    }
    return columns;
  }

  private String idSql() {
    return entity.id().strategy() == IdStrategy.IDENTITY
        ? idSqlType() + " GENERATED BY DEFAULT AS IDENTITY"
        : idSqlType();
  }
}
