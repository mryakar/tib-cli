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
import java.util.SortedSet;
import java.util.TreeSet;
import me.yakar.tib.spec.Names;

/**
 * The tests of an entity as the templates see them: their names, the sample values, and what the
 * tests import.
 */
public record EntityTestsModel(EntityModel entity) {

  /** The article of the name, for the names of the tests: {@code A} or {@code An}. */
  public String article() {
    return "AEIOU".indexOf(entity.name().charAt(0)) < 0 ? "A" : "An";
  }

  /**
   * The entities that an entity of this one needs before it is saved: the targets of its required
   * relations, and theirs, each after the ones it needs. A spec has no cycle of relations, so this
   * one is never among them.
   */
  public List<EntityModel> requiredTargets() {
    List<EntityModel> targets = new ArrayList<>();
    addRequiredTargets(entity, targets);
    return targets;
  }

  private static void addRequiredTargets(EntityModel model, List<EntityModel> targets) {
    for (RelationModel relation : model.requiredRelations()) {
      EntityModel target = relation.target();
      if (!targets.contains(target)) {
        addRequiredTargets(target, targets);
        targets.add(target);
      }
    }
  }

  /** The name of the collection as a type: {@code Vehicles}. */
  public String pluralType() {
    return Names.property(entity.plural());
  }

  /** Whether the entity has a DTO, and so do its samples. */
  public boolean dto() {
    return entity.dto();
  }

  /**
   * The parameters of the samples of the entity: the saved targets of its required relations, as in
   * {@code Depot depot}.
   */
  public String requiredParameters() {
    return String.join(
        ", ",
        entity.requiredRelations().stream()
            .map(relation -> relation.target().name() + " " + relation.field())
            .toList());
  }

  /**
   * The parameters of the DTO samples of the entity: the identifiers of the targets of its required
   * relations, as in {@code Long depotId}.
   */
  public String requiredIdParameters() {
    return String.join(
        ", ",
        entity.requiredRelations().stream()
            .map(relation -> relation.target().idType() + " " + relation.idField())
            .toList());
  }

  /**
   * The arguments of the samples of an entity in a test that saved what the entity needs first,
   * each in the variable of its entity.
   */
  public static String requiredArguments(EntityModel model) {
    return String.join(
        ", ",
        model.requiredRelations().stream().map(relation -> relation.target().variable()).toList());
  }

  /** The arguments of the DTO samples in a test that saved what the entity needs first. */
  public String requiredIdArguments() {
    return String.join(
        ", ",
        entity.requiredRelations().stream()
            .map(relation -> relation.target().variable() + ".getId()")
            .toList());
  }

  /**
   * The arguments of the DTO samples in the test of the service, which uses no database: a sample
   * identifier for every required relation.
   */
  public String sampleIdArguments() {
    return String.join(
        ", ",
        entity.requiredRelations().stream()
            .map(relation -> new EntityTestsModel(relation.target()).sampleId())
            .toList());
  }

  /** The arguments of the DTO that its builder builds: every value, and no audit times. */
  public String dtoBuilderArguments() {
    List<String> arguments = new ArrayList<>();
    arguments.add(entity.idField());
    entity.fields().forEach(field -> arguments.add(field.name()));
    entity.relations().forEach(relation -> arguments.add(relation.idField()));
    auditArguments(arguments);
    return String.join(", ", arguments);
  }

  /**
   * The imports of the samples: the types of the values and of the targets of the relations; the
   * formatter leaves out those that the samples do not use.
   */
  public String samplesImports() {
    SortedSet<String> imports = entity.typeImports();
    entity.relationIdImports(imports);
    for (RelationModel relation : entity.relations()) {
      EntityModel target = relation.target();
      if (!target.packageName().equals(entity.packageName())) {
        imports.add(target.packageName() + "." + target.name());
      }
    }
    return EntityModel.imports(imports);
  }

  /** Whether a DTO without values breaks a constraint: a field or a relation is not nullable. */
  public boolean requiresValues() {
    return entity.fields().stream().anyMatch(field -> !field.nullable())
        || !entity.requiredRelations().isEmpty();
  }

  /**
   * Whether two entities with the same sample values break a constraint: a field is unique, or a
   * one-to-one relation is not nullable.
   */
  public boolean unique() {
    return entity.fields().stream().anyMatch(field -> field.field().unique())
        || entity.requiredRelations().stream().anyMatch(RelationModel::oneToOne);
  }

  /** The fields of the DTO that the comparison of two of them leaves out, as Java strings. */
  public String ignoredFields(boolean id) {
    List<String> ignored = new ArrayList<>();
    if (id) {
      ignored.add(entity.idField());
    }
    if (entity.audited()) {
      ignored.add("createdAt");
      ignored.add("updatedAt");
    }
    return String.join(", ", ignored.stream().map(name -> "\"" + name + "\"").toList());
  }

  /** An identifier for the tests, as Java. */
  public String sampleId() {
    return entity.uuid() ? FieldModel.uuid(1) : "1L";
  }

  /** An identifier that no entity has, as Java. */
  public String unknownId() {
    return entity.uuid() ? "UUID.randomUUID()" : "Long.MAX_VALUE";
  }

  public String repositoryTestsImports() {
    SortedSet<String> statics = new TreeSet<>();
    statics.add("org.assertj.core.api.Assertions.assertThat");
    SortedSet<String> imports = containerTestsImports();
    if (unique()) {
      statics.add("org.assertj.core.api.Assertions.assertThatThrownBy");
      imports.add("org.springframework.dao.DataIntegrityViolationException");
    }
    imports.add("org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest");
    imports.add("org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase");
    imports.add("org.springframework.boot.jpa.test.autoconfigure.TestEntityManager");
    return staticImports(statics) + EntityModel.imports(imports);
  }

  public String serviceTestsImports() {
    SortedSet<String> statics = new TreeSet<>();
    statics.add("org.assertj.core.api.Assertions.assertThat");
    statics.add("org.assertj.core.api.Assertions.assertThatThrownBy");
    statics.add("org.mockito.ArgumentMatchers.any");
    statics.add("org.mockito.ArgumentMatchers.eq");
    statics.add("org.mockito.Mockito.verify");
    statics.add("org.mockito.Mockito.when");
    SortedSet<String> imports = entity.typeImports();
    entity.relationIdImports(imports);
    imports.add("java.util.Optional");
    imports.add("org.junit.jupiter.api.Test");
    imports.add("org.junit.jupiter.api.extension.ExtendWith");
    imports.add("org.mockito.InjectMocks");
    imports.add("org.mockito.Mock");
    imports.add("org.mockito.junit.jupiter.MockitoExtension");
    imports.add("org.springframework.http.HttpStatus");
    imports.add("org.springframework.web.server.ResponseStatusException");
    return staticImports(statics) + EntityModel.imports(imports);
  }

  public String apiTestsImports() {
    SortedSet<String> statics = new TreeSet<>();
    statics.add("org.assertj.core.api.Assertions.assertThat");
    SortedSet<String> imports = containerTestsImports();
    imports.addAll(entity.idImports());
    imports.add("org.junit.jupiter.api.AfterEach");
    imports.add("org.springframework.boot.resttestclient.TestRestTemplate");
    imports.add(
        "org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate");
    imports.add("org.springframework.boot.test.context.SpringBootTest");
    imports.add("tools.jackson.databind.JsonNode");
    imports.add("org.springframework.http.HttpEntity");
    imports.add("org.springframework.http.HttpMethod");
    imports.add("org.springframework.http.HttpStatus");
    imports.add("org.springframework.http.ResponseEntity");
    return staticImports(statics) + EntityModel.imports(imports);
  }

  /** Two nulls for the audit times of a DTO, if the entity has them. */
  private void auditArguments(List<String> arguments) {
    if (entity.audited()) {
      arguments.add("null");
      arguments.add("null");
    }
  }

  /**
   * The imports of a test against PostgreSQL in a container: the sample values of this entity and
   * of the entities it needs, those entities, their repositories and samples, and what saves them
   * first.
   */
  private SortedSet<String> containerTestsImports() {
    SortedSet<String> imports = EntityModel.fieldImports(entity.fields());
    for (EntityModel target : requiredTargets()) {
      imports.addAll(EntityModel.fieldImports(target.fields()));
      imports.add(target.packageName() + "." + target.name());
      imports.add(target.packageName() + "." + target.name() + "Repository");
      imports.add(target.packageName() + "." + target.name() + "Samples");
    }
    if (!requiredTargets().isEmpty()) {
      imports.add("org.junit.jupiter.api.BeforeEach");
    }
    imports.add(entity.project().basePackage() + ".TestcontainersConfiguration");
    imports.add("org.junit.jupiter.api.Test");
    imports.add("org.springframework.beans.factory.annotation.Autowired");
    imports.add("org.springframework.context.annotation.Import");
    return imports;
  }

  /** The static import statements, and a blank line after them. */
  private static String staticImports(SortedSet<String> imports) {
    StringBuilder text = new StringBuilder();
    imports.forEach(imported -> text.append("import static ").append(imported).append(";\n"));
    return text.append('\n').toString();
  }
}
