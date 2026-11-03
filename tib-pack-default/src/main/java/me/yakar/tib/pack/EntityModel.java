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
import java.util.Locale;
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
  String directory() {
    return "src/main/java/" + packageName().replace('.', '/');
  }

  /** The directory of the package's tests, relative to the project. */
  String testDirectory() {
    return "src/test/java/" + packageName().replace('.', '/');
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
                  + "\", allocationSize = 50)");
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
    for (RelationModel relation : relations) {
      if (relation.target().entity().id().type() == IdType.UUID) {
        imports.add("java.util.UUID");
      }
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

  /** The arguments of the DTO's constructor, from the entity. */
  public List<String> dtoArguments() {
    String it = variable();
    List<String> arguments = new ArrayList<>();
    arguments.add(it + ".getId()");
    for (FieldModel field : fields) {
      arguments.add(it + ".get" + field.property() + "()");
    }
    for (RelationModel relation : relations) {
      String target = it + ".get" + relation.property() + "()";
      arguments.add(target + " == null ? null : " + target + ".getId()");
    }
    if (entity.audit()) {
      arguments.add(it + ".getCreatedAt()");
      arguments.add(it + ".getUpdatedAt()");
    }
    return arguments;
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

  // The tests.

  /** The article of the name, for the names of the tests: {@code A} or {@code An}. */
  public String article() {
    return "AEIOU".indexOf(entity.name().charAt(0)) < 0 ? "A" : "An";
  }

  /**
   * The entities that an entity of this one needs before it is saved: the targets of its relations
   * that are not nullable, and theirs, each after the ones it needs. A relation to itself has to be
   * nullable, so this one is never among them.
   */
  public List<EntityModel> requiredTargets() {
    List<EntityModel> targets = new ArrayList<>();
    addRequiredTargets(targets);
    targets.remove(this);
    return targets;
  }

  private void addRequiredTargets(List<EntityModel> targets) {
    for (RelationModel relation : relations) {
      EntityModel target = relation.target();
      if (!relation.nullable() && target != this && !targets.contains(target)) {
        target.addRequiredTargets(targets);
        targets.add(target);
      }
    }
  }

  /** The entities that an entity of this one needs, each before the ones it needs. */
  public List<EntityModel> reversedRequiredTargets() {
    return requiredTargets().reversed();
  }

  /** The name of the collection as a type: {@code Vehicles}. */
  public String pluralType() {
    return Names.property(plural());
  }

  /**
   * The statements that give a new entity, in its variable, the first sample values: every field,
   * and each relation that is not nullable, to the variable of its target.
   */
  public List<String> sampleSetters() {
    String it = variable();
    List<String> setters = new ArrayList<>();
    for (FieldModel field : fields) {
      setters.add(it + ".set" + field.property() + "(" + field.sample(1) + ");");
    }
    for (RelationModel relation : requiredRelations()) {
      setters.add(it + ".set" + relation.property() + "(" + relation.target().variable() + ");");
    }
    return setters;
  }

  /** The assertions on the entity in {@code found}: it has the first sample values. */
  public List<String> foundAssertions() {
    List<String> assertions = new ArrayList<>();
    for (FieldModel field : fields) {
      assertions.add(
          "assertThat(found.get" + field.property() + "()).isEqualTo(" + field.sample(1) + ");");
    }
    for (RelationModel relation : requiredRelations()) {
      assertions.add(
          "assertThat(found.get"
              + relation.property()
              + "().getId()).isEqualTo("
              + relation.target().variable()
              + ".getId());");
    }
    if (entity.audit()) {
      assertions.add("assertThat(found.getCreatedAt()).isNotNull();");
      assertions.add("assertThat(found.getUpdatedAt()).isNotNull();");
    }
    return assertions;
  }

  /**
   * The arguments of a DTO that the API takes, with the first or the second sample values: no
   * identifier, a relation that is not nullable to the saved target in its variable, the others to
   * none, and no audit times.
   */
  public List<String> dtoSamples(int variant) {
    List<String> arguments = new ArrayList<>();
    arguments.add("null");
    for (FieldModel field : fields) {
      arguments.add(field.sample(variant));
    }
    for (RelationModel relation : relations) {
      arguments.add(relation.nullable() ? "null" : relation.target().variable() + ".getId()");
    }
    auditArguments(arguments);
    return arguments;
  }

  /**
   * The arguments of a DTO for the test of the service, which uses no database: the identifier
   * {@code ID}, the first sample values, and a sample identifier for every relation.
   */
  public List<String> serviceDtoArguments() {
    List<String> arguments = new ArrayList<>();
    arguments.add("ID");
    for (FieldModel field : fields) {
      arguments.add(field.sample(1));
    }
    for (RelationModel relation : relations) {
      arguments.add(relation.target().sampleId());
    }
    auditArguments(arguments);
    return arguments;
  }

  /** The arguments of a DTO without any value. */
  public List<String> emptyDtoArguments() {
    return dtoComponents().stream().map(component -> "null").toList();
  }

  /** Whether a DTO without values breaks a constraint: a field or a relation is not nullable. */
  public boolean requiresValues() {
    return fields.stream().anyMatch(field -> !field.nullable()) || !requiredRelations().isEmpty();
  }

  /**
   * Whether two entities with the same sample values break a constraint: a field is unique, or a
   * one-to-one relation is not nullable.
   */
  public boolean unique() {
    return fields.stream().anyMatch(field -> field.field().unique())
        || requiredRelations().stream().anyMatch(RelationModel::oneToOne);
  }

  /** The fields of the DTO that the comparison of two of them leaves out, as Java strings. */
  public String ignoredFields(boolean id) {
    List<String> ignored = new ArrayList<>();
    if (id) {
      ignored.add(idField());
    }
    if (entity.audit()) {
      ignored.add("createdAt");
      ignored.add("updatedAt");
    }
    return String.join(", ", ignored.stream().map(name -> "\"" + name + "\"").toList());
  }

  /** An identifier for the tests, as Java. */
  public String sampleId() {
    return entity.id().type() == IdType.UUID ? FieldModel.uuid(1) : "1L";
  }

  /** An identifier that no entity has, as Java. */
  public String unknownId() {
    return entity.id().type() == IdType.UUID ? "UUID.randomUUID()" : "Long.MAX_VALUE";
  }

  public String repositoryTestsImports() {
    SortedSet<String> statics = new TreeSet<>();
    statics.add("org.assertj.core.api.Assertions.assertThat");
    SortedSet<String> imports = sampleImports();
    imports.add(project.basePackage() + ".TestcontainersConfiguration");
    targetImports(imports);
    if (!requiredTargets().isEmpty()) {
      imports.add("org.junit.jupiter.api.BeforeEach");
    }
    if (unique()) {
      statics.add("org.assertj.core.api.Assertions.assertThatThrownBy");
      imports.add("org.springframework.dao.DataIntegrityViolationException");
    }
    imports.add("org.junit.jupiter.api.Test");
    imports.add("org.springframework.beans.factory.annotation.Autowired");
    imports.add("org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase");
    imports.add("org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest");
    imports.add("org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager");
    imports.add("org.springframework.context.annotation.Import");
    return staticImports(statics) + imports(imports);
  }

  public String serviceTestsImports() {
    SortedSet<String> statics = new TreeSet<>();
    statics.add("org.assertj.core.api.Assertions.assertThat");
    statics.add("org.assertj.core.api.Assertions.assertThatThrownBy");
    statics.add("org.mockito.ArgumentMatchers.any");
    statics.add("org.mockito.ArgumentMatchers.eq");
    statics.add("org.mockito.Mockito.verify");
    statics.add("org.mockito.Mockito.when");
    SortedSet<String> imports = typeImports();
    for (RelationModel relation : relations) {
      if (relation.target().entity().id().type() == IdType.UUID) {
        imports.add("java.util.UUID");
      }
    }
    imports.add("java.util.Optional");
    imports.add("org.junit.jupiter.api.Test");
    imports.add("org.junit.jupiter.api.extension.ExtendWith");
    imports.add("org.mockito.InjectMocks");
    imports.add("org.mockito.Mock");
    imports.add("org.mockito.junit.jupiter.MockitoExtension");
    imports.add("org.springframework.http.HttpStatus");
    imports.add("org.springframework.web.server.ResponseStatusException");
    return staticImports(statics) + imports(imports);
  }

  public String apiTestsImports() {
    SortedSet<String> statics = new TreeSet<>();
    statics.add("org.assertj.core.api.Assertions.assertThat");
    SortedSet<String> imports = sampleImports();
    imports.add(project.basePackage() + ".TestcontainersConfiguration");
    targetImports(imports);
    if (!requiredTargets().isEmpty()) {
      imports.add("org.junit.jupiter.api.BeforeEach");
    }
    if (entity.id().type() == IdType.UUID) {
      imports.add("java.util.UUID");
    }
    imports.add("com.fasterxml.jackson.databind.JsonNode");
    imports.add("org.junit.jupiter.api.AfterEach");
    imports.add("org.junit.jupiter.api.Test");
    imports.add("org.springframework.beans.factory.annotation.Autowired");
    imports.add("org.springframework.boot.test.context.SpringBootTest");
    imports.add("org.springframework.boot.test.web.client.TestRestTemplate");
    imports.add("org.springframework.context.annotation.Import");
    imports.add("org.springframework.http.HttpEntity");
    imports.add("org.springframework.http.HttpMethod");
    imports.add("org.springframework.http.HttpStatus");
    imports.add("org.springframework.http.ResponseEntity");
    return staticImports(statics) + imports(imports);
  }

  /** The relations that are not nullable, but for one to itself, which has to be. */
  private List<RelationModel> requiredRelations() {
    return relations.stream()
        .filter(relation -> !relation.nullable() && relation.target() != this)
        .toList();
  }

  /** Two nulls for the audit times of a DTO, if the entity has them. */
  private void auditArguments(List<String> arguments) {
    if (entity.audit()) {
      arguments.add("null");
      arguments.add("null");
    }
  }

  /** The imports of the sample values of this entity and of the entities it needs. */
  private SortedSet<String> sampleImports() {
    SortedSet<String> imports = fieldImports(fields);
    for (EntityModel target : requiredTargets()) {
      imports.addAll(fieldImports(target.fields));
    }
    return imports;
  }

  /** The imports of the entities that this one needs, and of their repositories. */
  private void targetImports(SortedSet<String> imports) {
    for (EntityModel target : requiredTargets()) {
      imports.add(target.packageName() + "." + target.name());
      imports.add(target.packageName() + "." + target.name() + "Repository");
    }
  }

  // Imports.

  /** The import of the identifier's type, if it needs one. */
  private SortedSet<String> idImports() {
    SortedSet<String> imports = new TreeSet<>();
    if (entity.id().type() == IdType.UUID) {
      imports.add("java.util.UUID");
    }
    return imports;
  }

  /** The imports of the identifier's type and of the fields' types. */
  private SortedSet<String> typeImports() {
    SortedSet<String> imports = idImports();
    imports.addAll(fieldImports(fields));
    return imports;
  }

  /** The imports of the types of the fields. */
  private static SortedSet<String> fieldImports(List<FieldModel> fields) {
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

  /** The static import statements, and a blank line after them. */
  private static String staticImports(SortedSet<String> imports) {
    StringBuilder text = new StringBuilder();
    imports.forEach(imported -> text.append("import static ").append(imported).append(";\n"));
    return text.append('\n').toString();
  }

  /** The import statements, and a blank line after them; nothing when there are none. */
  private static String imports(SortedSet<String> imports) {
    if (imports.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    imports.forEach(imported -> text.append("import ").append(imported).append(";\n"));
    return text.append('\n').toString();
  }
}
