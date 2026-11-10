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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.yakar.tib.spec.Names;

/**
 * The tests of an entity as the templates see them: their names, and the samples that they take.
 */
public final class EntityTestsModel {

  /** A value that a builder of the samples takes: its Java type and its name. */
  public record Property(String type, String name) {}

  private final EntityModel entity;
  private final List<EntityModel> requiredTargets;

  EntityTestsModel(EntityModel entity) {
    this.entity = entity;
    List<EntityModel> targets = new ArrayList<>();
    addRequiredTargets(entity, targets);
    this.requiredTargets = List.copyOf(targets);
  }

  public EntityModel entity() {
    return entity;
  }

  /**
   * The entities that an entity of this one needs before it is saved: the targets of its required
   * relations, and theirs, each after the ones it needs. A spec has no cycle of relations, so this
   * one is never among them.
   */
  public List<EntityModel> requiredTargets() {
    return requiredTargets;
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

  /**
   * The parameters of the samples of the entity: the saved targets of its required relations, as in
   * {@code Depot depot}.
   */
  public String requiredParameters() {
    return required(entity, relation -> relation.target().name() + " " + relation.field());
  }

  /**
   * The parameters of the DTO samples of the entity: the identifiers of the targets of its required
   * relations, as in {@code Long depotId}.
   */
  public String requiredIdParameters() {
    return required(entity, relation -> relation.target().idType() + " " + relation.idField());
  }

  /**
   * A sample of an entity, built, in a test that saved what the entity needs first, each in the
   * variable of its entity: {@code VehicleSamples.vehicle(depot).build()}.
   */
  public String sample(EntityModel model) {
    return model.name()
        + "Samples."
        + model.variable()
        + "("
        + required(model, relation -> relation.target().variable())
        + ").build()";
  }

  /** The arguments of the DTO samples in a test that saved what the entity needs first. */
  public String requiredIdArguments() {
    return required(entity, relation -> relation.target().variable() + ".getId()");
  }

  /**
   * The arguments of the DTO samples in the test of the service, which uses no database: a sample
   * identifier for every required relation.
   */
  public String sampleIdArguments() {
    return required(entity, relation -> sampleId(relation.target()));
  }

  /** What the function makes of each required relation of the entity, with commas between. */
  private static String required(EntityModel model, Function<RelationModel, String> text) {
    return model.requiredRelations().stream().map(text).collect(Collectors.joining(", "));
  }

  /**
   * The calls that give a builder of the entity the first or the second sample values, and the
   * saved targets of its required relations: {@code .name("One").depot(depot)}.
   */
  public String sampleValues(int variant) {
    return fieldValues(variant)
        + entity.requiredRelations().stream()
            .map(relation -> "." + relation.field() + "(" + relation.field() + ")")
            .collect(Collectors.joining());
  }

  /**
   * The calls that give a builder of the DTO the first or the second sample values, and the
   * identifiers of the targets of its required relations: {@code .name("One").depotId(depotId)}.
   */
  public String dtoSampleValues(int variant) {
    return fieldValues(variant)
        + entity.requiredRelations().stream()
            .map(relation -> "." + relation.idField() + "(" + relation.idField() + ")")
            .collect(Collectors.joining());
  }

  private String fieldValues(int variant) {
    return entity.fields().stream()
        .map(field -> "." + field.name() + "(" + field.sample(variant) + ")")
        .collect(Collectors.joining());
  }

  /** What the builder of the entity takes: every field, and the target of every relation. */
  public List<Property> builderProperties() {
    return Stream.concat(
            fieldProperties(),
            entity.relations().stream()
                .map(relation -> new Property(relation.target().name(), relation.field())))
        .toList();
  }

  /**
   * What the builder of the DTO takes: the identifier, every field, and the identifier of the
   * target of every relation.
   */
  public List<Property> dtoBuilderProperties() {
    List<Property> properties = new ArrayList<>();
    properties.add(new Property(entity.idType(), entity.idField()));
    fieldProperties().forEach(properties::add);
    for (RelationModel relation : entity.relations()) {
      properties.add(new Property(relation.target().idType(), relation.idField()));
    }
    return properties;
  }

  private Stream<Property> fieldProperties() {
    return entity.fields().stream().map(field -> new Property(field.javaType(), field.name()));
  }

  /**
   * The arguments of the DTO that its builder builds: every value, and null for the audit times.
   */
  public String dtoBuilderArguments() {
    List<String> arguments = new ArrayList<>();
    dtoBuilderProperties().forEach(property -> arguments.add(property.name()));
    if (entity.audited()) {
      arguments.add("null");
      arguments.add("null");
    }
    return String.join(", ", arguments);
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
    return entity.fields().stream().anyMatch(FieldModel::unique)
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
    return ignored.stream().map(name -> "\"" + name + "\"").collect(Collectors.joining(", "));
  }

  /** An identifier for the tests, as Java. */
  public String sampleId() {
    return sampleId(entity);
  }

  private static String sampleId(EntityModel model) {
    return model.uuid() ? FieldModel.uuid(1) : "1L";
  }

  /** An identifier that no entity has, as Java. */
  public String unknownId() {
    return entity.uuid() ? "UUID.randomUUID()" : "Long.MAX_VALUE";
  }
}
