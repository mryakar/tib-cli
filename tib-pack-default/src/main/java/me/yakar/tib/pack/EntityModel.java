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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import me.yakar.tib.generate.Project;
import me.yakar.tib.spec.Entity;
import me.yakar.tib.spec.Field;
import me.yakar.tib.spec.IdStrategy;
import me.yakar.tib.spec.IdType;
import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Relation;
import me.yakar.tib.spec.RelationKind;

/**
 * An entity as the templates see it: its names, and the blocks of code that depend on its fields,
 * relations and options, worked out here so that the templates only print them and the layout of
 * the generated code is exact.
 */
public final class EntityModel {

  private final Project project;
  private final Entity entity;
  private final Map<String, Entity> entities;

  EntityModel(Project project, Entity entity, Map<String, Entity> entities) {
    this.project = project;
    this.entity = entity;
    this.entities = entities;
  }

  public Entity entity() {
    return entity;
  }

  public String name() {
    return entity.name();
  }

  /** The name as a variable: {@code vehicle}. */
  public String variable() {
    return Names.member(entity.name());
  }

  /** The name of the collection, for the path of the API: {@code vehicles}. */
  public String plural() {
    return Names.plural(variable());
  }

  public String table() {
    return entity.table();
  }

  /** The package of the entity's classes: one per entity, under the base package. */
  public String packageName() {
    return project.basePackage() + "." + entity.name().toLowerCase(Locale.ROOT);
  }

  /** The directory of the package, relative to the project. */
  public String directory() {
    return "src/main/java/" + packageName().replace('.', '/');
  }

  public String idField() {
    return entity.id().field();
  }

  public String idColumn() {
    return Names.sql(entity.id().field());
  }

  /** The Java type of the identifier: {@code Long} or {@code UUID}. */
  public String idType() {
    return entity.id().type().toString();
  }

  public boolean idSequence() {
    return entity.id().strategy() == IdStrategy.SEQUENCE;
  }

  /** The database sequence of a sequence identifier. */
  public String sequence() {
    return entity.table() + "_SEQ";
  }

  public boolean dto() {
    return entity.options().dto();
  }

  /** Whether the service and the controller are generated: they need the DTO and the mapper. */
  public boolean layer() {
    return entity.options().dto() && entity.options().mapper();
  }

  List<FieldModel> fields() {
    return entity.fields().stream().map(FieldModel::new).toList();
  }

  List<RelationModel> relations() {
    return entity.relations().stream()
        .map(
            relation ->
                new RelationModel(
                    relation, new EntityModel(project, entities.get(relation.target()), entities)))
        .toList();
  }

  /**
   * The entities the service needs the repositories of: the targets of its relations, once each.
   */
  List<EntityModel> targets() {
    List<EntityModel> targets = new ArrayList<>();
    for (RelationModel relation : relations()) {
      String target = relation.target().name();
      if (!target.equals(name()) && targets.stream().noneMatch(t -> t.name().equals(target))) {
        targets.add(relation.target());
      }
    }
    return targets;
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
    for (RelationModel relation : relations()) {
      imports.add("jakarta.persistence.FetchType");
      imports.add("jakarta.persistence.JoinColumn");
      imports.add("jakarta.persistence." + relation.annotation());
      if (!relation.target().name().equals(name())) {
        imports.add(relation.target().packageName() + "." + relation.target().name());
      }
    }
    if (entity.audit()) {
      imports.add("java.time.Instant");
      imports.add("org.hibernate.annotations.CreationTimestamp");
      imports.add("org.hibernate.annotations.UpdateTimestamp");
    }
    return imports(imports);
  }

  /** The fields of the class with their annotations, a blank line between them. */
  public String declarations() {
    List<List<String>> blocks = new ArrayList<>();
    List<String> id = new ArrayList<>(idAnnotations());
    id.add("@Column(name = \"" + idColumn() + "\", nullable = false)");
    id.add("private " + idType() + " " + idField() + ";");
    blocks.add(id);
    for (FieldModel field : fields()) {
      blocks.add(
          List.of(
              field.columnAnnotation(), "private " + field.javaType() + " " + field.name() + ";"));
    }
    for (RelationModel relation : relations()) {
      List<String> block = new ArrayList<>(relation.annotations());
      block.add("private " + relation.target().name() + " " + relation.field() + ";");
      blocks.add(block);
    }
    if (entity.audit()) {
      blocks.add(
          List.of(
              "@CreationTimestamp",
              "@Column(name = \"CREATED_AT\", nullable = false, updatable = false)",
              "private Instant createdAt;"));
      blocks.add(
          List.of(
              "@UpdateTimestamp",
              "@Column(name = \"UPDATED_AT\", nullable = false)",
              "private Instant updatedAt;"));
    }
    return blocks(blocks, "  ");
  }

  /** The getters and setters, a blank line between them. */
  public String accessors() {
    List<List<String>> blocks = new ArrayList<>();
    blocks.add(getter(idType(), "Id", idField()));
    blocks.add(setter(idType(), "Id", idField()));
    for (FieldModel field : fields()) {
      blocks.add(getter(field.javaType(), field.property(), field.name()));
      blocks.add(setter(field.javaType(), field.property(), field.name()));
    }
    for (RelationModel relation : relations()) {
      blocks.add(getter(relation.target().name(), relation.property(), relation.field()));
      blocks.add(setter(relation.target().name(), relation.property(), relation.field()));
    }
    if (entity.audit()) {
      blocks.add(getter("Instant", "CreatedAt", "createdAt"));
      blocks.add(getter("Instant", "UpdatedAt", "updatedAt"));
    }
    return blocks(blocks, "  ");
  }

  private List<String> idAnnotations() {
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
                  + "\", allocationSize = 50)");
      case UUID -> List.of("@Id", "@GeneratedValue(strategy = GenerationType.UUID)");
    };
  }

  private static List<String> getter(String type, String property, String field) {
    return List.of("public " + type + " get" + property + "() {", "  return " + field + ";", "}");
  }

  private static List<String> setter(String type, String property, String field) {
    return List.of(
        "public void set" + property + "(" + type + " " + field + ") {",
        "  this." + field + " = " + field + ";",
        "}");
  }

  // The repository.

  public String repositoryImports() {
    SortedSet<String> imports = new TreeSet<>();
    if (entity.id().type() == IdType.UUID) {
      imports.add("java.util.UUID");
    }
    imports.add("org.springframework.data.jpa.repository.JpaRepository");
    return imports(imports);
  }

  // The DTO.

  public String dtoImports() {
    SortedSet<String> imports = typeImports();
    for (RelationModel relation : relations()) {
      if (relation.target().entity().id().type() == IdType.UUID) {
        imports.add("java.util.UUID");
      }
      if (!relation.nullable()) {
        imports.add("jakarta.validation.constraints.NotNull");
      }
    }
    for (FieldModel field : fields()) {
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

  /** The components of the record, one per line. */
  public String dtoComponents() {
    List<String> components = new ArrayList<>();
    components.add(idType() + " " + idField());
    for (FieldModel field : fields()) {
      components.add(field.validation() + field.javaType() + " " + field.name());
    }
    for (RelationModel relation : relations()) {
      components.add(relation.validation() + relation.target().idType() + " " + relation.idField());
    }
    if (entity.audit()) {
      components.add("Instant createdAt");
      components.add("Instant updatedAt");
    }
    return String.join(",\n    ", components);
  }

  // The mapper.

  /** The arguments of the DTO's constructor, from the entity, one per line. */
  public String mapperArguments() {
    String it = variable();
    List<String> arguments = new ArrayList<>();
    arguments.add(it + ".getId()");
    for (FieldModel field : fields()) {
      arguments.add(it + ".get" + field.property() + "()");
    }
    for (RelationModel relation : relations()) {
      String target = it + ".get" + relation.property() + "()";
      arguments.add(target + " == null ? null : " + target + ".getId()");
    }
    if (entity.audit()) {
      arguments.add(it + ".getCreatedAt()");
      arguments.add(it + ".getUpdatedAt()");
    }
    return String.join(",\n        ", arguments);
  }

  /** The statements that copy the fields of the DTO onto the entity. */
  public String mapperUpdates() {
    List<String> statements = new ArrayList<>();
    for (FieldModel field : fields()) {
      statements.add(variable() + ".set" + field.property() + "(dto." + field.name() + "());");
    }
    return lines(statements, "    ");
  }

  // The service.

  public String serviceImports() {
    SortedSet<String> imports = new TreeSet<>();
    if (entity.id().type() == IdType.UUID) {
      imports.add("java.util.UUID");
    }
    if (entity.options().pagination()) {
      imports.add("org.springframework.data.domain.Page");
      imports.add("org.springframework.data.domain.Pageable");
    } else {
      imports.add("java.util.List");
    }
    for (EntityModel target : targets()) {
      imports.add(target.packageName() + "." + target.name() + "Repository");
    }
    imports.add("org.springframework.http.HttpStatus");
    imports.add("org.springframework.stereotype.Service");
    imports.add("org.springframework.transaction.annotation.Transactional");
    imports.add("org.springframework.web.server.ResponseStatusException");
    return imports(imports);
  }

  /** The repositories of the targets, as fields of the service. */
  public String serviceFields() {
    List<String> fields = new ArrayList<>();
    for (EntityModel target : targets()) {
      fields.add("private final " + target.name() + "Repository " + target.plural() + ";");
    }
    return lines(fields, "  ");
  }

  /** The repositories of the targets, as parameters of the constructor, after the first ones. */
  public String serviceParameters() {
    StringBuilder parameters = new StringBuilder();
    for (EntityModel target : targets()) {
      parameters.append(", ").append(target.name()).append("Repository ").append(target.plural());
    }
    return parameters.toString();
  }

  public String serviceAssignments() {
    List<String> assignments = new ArrayList<>();
    for (EntityModel target : targets()) {
      assignments.add("this." + target.plural() + " = " + target.plural() + ";");
    }
    return lines(assignments, "    ");
  }

  /** The method that lists the entities: a page of them, or all of them. */
  public String serviceList() {
    String dto = name() + "Dto";
    List<String> method =
        entity.options().pagination()
            ? List.of(
                "@Transactional(readOnly = true)",
                "public Page<" + dto + "> list(Pageable pageable) {",
                "  return " + plural() + ".findAll(pageable).map(mapper::toDto);",
                "}")
            : List.of(
                "@Transactional(readOnly = true)",
                "public List<" + dto + "> list() {",
                "  return " + plural() + ".findAll().stream().map(mapper::toDto).toList();",
                "}");
    return lines(method, "  ");
  }

  /** The statements that resolve the relations of the DTO onto the entity. */
  public String serviceRelations() {
    List<String> statements = new ArrayList<>();
    for (RelationModel relation : relations()) {
      String repository =
          relation.target().name().equals(name()) ? plural() : relation.target().plural();
      statements.add(
          variable()
              + ".set"
              + relation.property()
              + "(dto."
              + relation.idField()
              + "() == null ? null : "
              + repository
              + ".getReferenceById(dto."
              + relation.idField()
              + "()));");
    }
    return lines(statements, "    ");
  }

  // The controller.

  public String controllerImports() {
    SortedSet<String> imports = new TreeSet<>();
    imports.add("jakarta.validation.Valid");
    if (entity.id().type() == IdType.UUID) {
      imports.add("java.util.UUID");
    }
    if (entity.options().pagination()) {
      imports.add("org.springframework.data.domain.Page");
      imports.add("org.springframework.data.domain.Pageable");
    } else {
      imports.add("java.util.List");
    }
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

  /** The endpoint that lists the entities: a page of them, or all of them. */
  public String controllerList() {
    String dto = name() + "Dto";
    List<String> method =
        entity.options().pagination()
            ? List.of(
                "@GetMapping",
                "public Page<" + dto + "> list(Pageable pageable) {",
                "  return service.list(pageable);",
                "}")
            : List.of(
                "@GetMapping",
                "public List<" + dto + "> list() {",
                "  return service.list();",
                "}");
    return lines(method, "  ");
  }

  // The migration.

  /** The statement that creates the sequence of a sequence identifier, or nothing. */
  public String sequenceStatement() {
    return idSequence()
        ? "CREATE SEQUENCE " + sequence() + " START WITH 1 INCREMENT BY 50;\n\n"
        : "";
  }

  /** The columns of the table, one definition per line. */
  public String columns() {
    List<String> columns = new ArrayList<>();
    columns.add(idColumn() + " " + idSql() + " PRIMARY KEY");
    for (FieldModel field : fields()) {
      columns.add(field.sql());
    }
    for (RelationModel relation : relations()) {
      columns.add(relation.sql());
    }
    if (entity.audit()) {
      columns.add("CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL");
      columns.add("UPDATED_AT TIMESTAMP WITH TIME ZONE NOT NULL");
    }
    return String.join(",\n  ", columns);
  }

  private String idSql() {
    return switch (entity.id().strategy()) {
      case IDENTITY -> "BIGINT GENERATED BY DEFAULT AS IDENTITY";
      case SEQUENCE -> "BIGINT";
      case UUID -> "UUID";
    };
  }

  // Text.

  private SortedSet<String> typeImports() {
    SortedSet<String> imports = new TreeSet<>();
    if (entity.id().type() == IdType.UUID) {
      imports.add("java.util.UUID");
    }
    for (Field field : entity.fields()) {
      switch (field.type()) {
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

  /** The import statements, and a blank line after them; nothing when there are none. */
  private static String imports(Collection<String> imports) {
    if (imports.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    imports.forEach(imported -> text.append("import ").append(imported).append(";\n"));
    return text.append('\n').toString();
  }

  /** The lines, each indented, each ended. */
  private static String lines(List<String> lines, String indent) {
    StringBuilder text = new StringBuilder();
    lines.forEach(line -> text.append(indent).append(line).append('\n'));
    return text.toString();
  }

  /** The blocks of lines, indented, a blank line between them, without a final line end. */
  private static String blocks(List<List<String>> blocks, String indent) {
    List<String> texts = new ArrayList<>();
    blocks.forEach(block -> texts.add(lines(block, indent).stripTrailing()));
    return String.join("\n\n", texts);
  }

  /** A field as the templates see it. */
  record FieldModel(Field field) {

    String name() {
      return field.name();
    }

    String javaType() {
      return field.type().toString();
    }

    boolean nullable() {
      return field.nullable();
    }

    Integer length() {
      return field.length();
    }

    /** The name with its first letter in upper case, for the accessors. */
    String property() {
      return Character.toUpperCase(field.name().charAt(0)) + field.name().substring(1);
    }

    /** The JPA annotation of the column. */
    String columnAnnotation() {
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

    /** The validation annotations of the DTO's component, in front of its type. */
    String validation() {
      StringBuilder validation = new StringBuilder();
      if (!field.nullable()) {
        validation.append("@NotNull ");
      }
      if (field.length() != null) {
        validation.append("@Size(max = ").append(field.length()).append(") ");
      }
      return validation.toString();
    }

    /** The definition of the column in SQL. */
    String sql() {
      StringBuilder sql = new StringBuilder(field.column() + " " + sqlType());
      if (!field.nullable()) {
        sql.append(" NOT NULL");
      }
      if (field.unique()) {
        sql.append(" UNIQUE");
      }
      return sql.toString();
    }

    private String sqlType() {
      return switch (field.type()) {
        case STRING -> "VARCHAR(" + (field.length() == null ? 255 : field.length()) + ")";
        case INTEGER -> "INTEGER";
        case LONG -> "BIGINT";
        case BIG_DECIMAL ->
            field.precision() == null
                ? "NUMERIC"
                : "NUMERIC("
                    + field.precision()
                    + ", "
                    + (field.scale() == null ? 0 : field.scale())
                    + ")";
        case BOOLEAN -> "BOOLEAN";
        case LOCAL_DATE -> "DATE";
        case LOCAL_DATE_TIME -> "TIMESTAMP";
        case INSTANT -> "TIMESTAMP WITH TIME ZONE";
        case UUID -> "UUID";
      };
    }
  }

  /** A relation as the templates see it, with its target. */
  record RelationModel(Relation relation, EntityModel target) {

    String field() {
      return relation.field();
    }

    boolean nullable() {
      return relation.nullable();
    }

    String property() {
      return Character.toUpperCase(relation.field().charAt(0)) + relation.field().substring(1);
    }

    /** The field of the DTO that holds the identifier of the target: {@code depotId}. */
    String idField() {
      String id = target.idField();
      return relation.field() + Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /** The JPA annotation of the relation's kind. */
    String annotation() {
      return relation.kind() == RelationKind.MANY_TO_ONE ? "ManyToOne" : "OneToOne";
    }

    /** The JPA annotations of the relation, one per line. */
    List<String> annotations() {
      boolean one = relation.kind() == RelationKind.ONE_TO_ONE;
      return List.of(
          "@"
              + annotation()
              + "(fetch = FetchType.LAZY"
              + (relation.nullable() ? "" : ", optional = false")
              + ")",
          "@JoinColumn(name = \""
              + relation.column()
              + "\""
              + (relation.nullable() ? "" : ", nullable = false")
              + (one ? ", unique = true" : "")
              + ")");
    }

    /** The validation annotation of the DTO's component, in front of its type. */
    String validation() {
      return relation.nullable() ? "" : "@NotNull ";
    }

    /** The definition of the column in SQL, with its foreign key. */
    String sql() {
      return relation.column()
          + " "
          + (target.entity().id().type() == IdType.UUID ? "UUID" : "BIGINT")
          + (relation.nullable() ? "" : " NOT NULL")
          + (relation.kind() == RelationKind.ONE_TO_ONE ? " UNIQUE" : "")
          + " REFERENCES "
          + target.table()
          + " ("
          + target.idColumn()
          + ")";
    }
  }
}
