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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.IdStrategy;
import me.yakar.tib.spec.IdType;
import me.yakar.tib.spec.Names;

/**
 * An entity as the templates see it: its names, its fields and relations, and what the generated
 * code imports and declares. The templates lay the code out.
 */
public final class EntityModel {

  /**
   * How many identifiers Hibernate takes from a sequence at a time: the increment of the sequence,
   * which Hibernate checks when it starts.
   */
  private static final int ALLOCATION_SIZE = 50;

  private final Project project;
  private final Entity entity;
  private final List<FieldModel> fields;
  private final List<RelationModel> relations;
  private final List<EntityModel> targets;

  /**
   * The model of an entity, after the models of the entities it refers to: those are in the map, by
   * name.
   */
  EntityModel(Project project, Entity entity, Map<String, EntityModel> models) {
    this.project = project;
    this.entity = entity;
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

  Project project() {
    return project;
  }

  Entity entity() {
    return entity;
  }

  public String name() {
    return entity.name();
  }

  /** The name as a variable: {@code vehicle}. */
  public String variable() {
    return Names.member(entity.name());
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

  /** The directory of the package, relative to the project. */
  String directory() {
    return DefaultPack.MAIN + packageName().replace('.', '/');
  }

  /** The directory of the package's tests, relative to the project. */
  String testDirectory() {
    return DefaultPack.TEST + packageName().replace('.', '/');
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

  /** Whether the identifier is a UUID. */
  boolean uuid() {
    return entity.id().type() == IdType.UUID;
  }

  public boolean idSequence() {
    return entity.id().strategy() == IdStrategy.SEQUENCE;
  }

  /** The database sequence of a sequence identifier. */
  public String sequence() {
    return entity.table() + "_SEQ";
  }

  /** The increment of the sequence: the allocation size of its generator. */
  public int sequenceIncrement() {
    return ALLOCATION_SIZE;
  }

  public boolean audited() {
    return entity.audit();
  }

  /** Whether the list of the entities comes in pages. */
  public boolean paginated() {
    return entity.options().pagination();
  }

  boolean dto() {
    return entity.options().dto();
  }

  /** Whether the service and the controller are generated: they need the DTO and the mapper. */
  boolean layer() {
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

  public String generatedImports() {
    SortedSet<String> imports = typeImports();
    imports.add("jakarta.persistence.Column");
    imports.add("jakarta.persistence.GeneratedValue");
    imports.add("jakarta.persistence.GenerationType");
    imports.add("jakarta.persistence.Id");
    imports.add("jakarta.persistence.MappedSuperclass");
    if (idSequence()) {
      imports.add("jakarta.persistence.SequenceGenerator");
    }
    for (RelationModel relation : relations) {
      imports.add("jakarta.persistence.FetchType");
      imports.add("jakarta.persistence.JoinColumn");
      imports.add("jakarta.persistence." + relation.annotation());
    }
    for (EntityModel target : targets) {
      if (target != this) {
        imports.add(target.packageName() + "." + target.name());
      }
    }
    if (entity.audit()) {
      imports.add("java.time.Instant");
      imports.add("org.hibernate.annotations.CreationTimestamp");
      imports.add("org.hibernate.annotations.UpdateTimestamp");
    }
    return imports(imports);
  }

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

  // The repository.

  public String repositoryImports() {
    SortedSet<String> imports = idImports();
    imports.add("org.springframework.data.jpa.repository.JpaRepository");
    return imports(imports);
  }

  // The DTO.

  public String dtoImports() {
    SortedSet<String> imports = typeImports();
    relationIdImports(imports);
    for (RelationModel relation : relations) {
      if (!relation.nullable()) {
        imports.add("jakarta.validation.constraints.NotNull");
      }
    }
    for (FieldModel field : fields) {
      if (!field.nullable()) {
        imports.add("jakarta.validation.constraints.NotNull");
      }
      if (field.length() != null) {
        imports.add("jakarta.validation.constraints.Size");
      }
    }
    if (entity.audit()) {
      imports.add("java.time.Instant");
    }
    return imports(imports);
  }

  /** The components of the record: its type and name, after their validation. */
  public List<String> dtoComponents() {
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
    return components;
  }

  // The mapper.

  public String mapperImports() {
    SortedSet<String> imports = new TreeSet<>();
    for (EntityModel target : targets) {
      if (target != this) {
        imports.add(target.packageName() + "." + target.name() + "Repository");
      }
    }
    imports.add("org.springframework.stereotype.Component");
    return imports(imports);
  }

  // The service.

  public String serviceImports() {
    SortedSet<String> imports = idImports();
    listImports(imports);
    imports.add("org.springframework.http.HttpStatus");
    imports.add("org.springframework.stereotype.Service");
    imports.add("org.springframework.transaction.annotation.Transactional");
    imports.add("org.springframework.web.server.ResponseStatusException");
    return imports(imports);
  }

  // The controller.

  public String controllerImports() {
    SortedSet<String> imports = idImports();
    imports.add("jakarta.validation.Valid");
    listImports(imports);
    imports.add("org.springframework.http.HttpStatus");
    for (String annotation :
        List.of(
            "DeleteMapping",
            "GetMapping",
            "PathVariable",
            "PostMapping",
            "PutMapping",
            "RequestBody",
            "RequestMapping",
            "ResponseStatus",
            "RestController")) {
      imports.add("org.springframework.web.bind.annotation." + annotation);
    }
    return imports(imports);
  }

  // The migration.

  /**
   * The relations whose columns get an index: the many-to-one ones. A one-to-one column is unique,
   * which indexes it.
   */
  public List<RelationModel> indexed() {
    return relations.stream().filter(relation -> !relation.oneToOne()).toList();
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
      columns.add(Entity.CREATED_AT + " TIMESTAMP WITH TIME ZONE NOT NULL");
      columns.add(Entity.UPDATED_AT + " TIMESTAMP WITH TIME ZONE NOT NULL");
    }
    return columns;
  }

  private String idSql() {
    String type = FieldModel.sqlType(entity.id().type());
    return entity.id().strategy() == IdStrategy.IDENTITY
        ? type + " GENERATED BY DEFAULT AS IDENTITY"
        : type;
  }

  // Imports.

  /** The import of the identifier's type, if it needs one. */
  SortedSet<String> idImports() {
    SortedSet<String> imports = new TreeSet<>();
    if (uuid()) {
      imports.add("java.util.UUID");
    }
    return imports;
  }

  /** The import of the type of the identifiers of the relations' targets, if they need one. */
  void relationIdImports(SortedSet<String> imports) {
    if (relations.stream().anyMatch(relation -> relation.target().uuid())) {
      imports.add("java.util.UUID");
    }
  }

  /** The imports of the identifier's type and of the fields' types. */
  SortedSet<String> typeImports() {
    SortedSet<String> imports = idImports();
    imports.addAll(fieldImports(fields));
    return imports;
  }

  /** The imports of the types of the fields. */
  static SortedSet<String> fieldImports(List<FieldModel> fields) {
    SortedSet<String> imports = new TreeSet<>();
    for (FieldModel field : fields) {
      switch (field.field().type()) {
        case BIG_DECIMAL -> imports.add("java.math.BigDecimal");
        case LOCAL_DATE -> imports.add("java.time.LocalDate");
        case LOCAL_DATE_TIME -> imports.add("java.time.LocalDateTime");
        case INSTANT -> imports.add("java.time.Instant");
        case UUID -> imports.add("java.util.UUID");
        default -> {}
      }
    }
    return imports;
  }

  /** The imports of a list of the entities: a page of them, or all of them. */
  private void listImports(SortedSet<String> imports) {
    if (paginated()) {
      imports.add("org.springframework.data.domain.Page");
      imports.add("org.springframework.data.domain.Pageable");
    } else {
      imports.add("java.util.List");
    }
  }

  /** The import statements, and a blank line after them; nothing when there are none. */
  static String imports(SortedSet<String> imports) {
    if (imports.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    imports.forEach(imported -> text.append("import ").append(imported).append(";\n"));
    return text.append('\n').toString();
  }
}
