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
import java.util.Optional;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.FieldModel;
import me.yakar.tib.pack.RelationModel;

/**
 * The DTO of an entity as the templates of the plain pack see it: a record without annotations,
 * which checks the rules of the spec itself, as no framework validates it.
 */
public final class DtoModel {

  /**
   * A rule of the spec that a value of the DTO keeps.
   *
   * @param name the component that holds the value
   * @param required whether the value is required
   * @param length the length that a String may not exceed, or null
   */
  public record Check(String name, boolean required, Integer length) {}

  /**
   * A value that the DTO takes and its column does not.
   *
   * @param name the component that holds the value
   * @param value the value, as Java
   */
  public record TooLarge(String name, String value) {}

  private final EntityModel entity;

  public DtoModel(EntityModel entity) {
    this.entity = entity;
  }

  /**
   * The components of the record: the identifier, every field, the identifier of the target of
   * every relation, and the audit times of an audited entity.
   */
  public String components() {
    List<String> components = new ArrayList<>();
    components.add(entity.idType() + " " + entity.idField());
    for (FieldModel field : entity.fields()) {
      components.add(field.javaType() + " " + field.name());
    }
    for (RelationModel relation : entity.relations()) {
      components.add(relation.target().idType() + " " + relation.idField());
    }
    if (entity.audited()) {
      components.add("Instant createdAt");
      components.add("Instant updatedAt");
    }
    return String.join(", ", components);
  }

  /**
   * The rules that the DTO checks, in the order of its components: the fields that are required or
   * that are Strings of a length, and the relations that are required.
   */
  public List<Check> checks() {
    List<Check> checks = new ArrayList<>();
    for (FieldModel field : entity.fields()) {
      if (!field.nullable() || field.length() != null) {
        checks.add(new Check(field.name(), !field.nullable(), field.length()));
      }
    }
    for (RelationModel relation : entity.relations()) {
      if (!relation.nullable()) {
        checks.add(new Check(relation.idField(), true, null));
      }
    }
    return checks;
  }

  /**
   * A value that keeps the rules of the DTO and does not fit its column, if a field has one: a
   * BigDecimal with a digit more before the point than the precision of its column has.
   */
  public Optional<TooLarge> tooLarge() {
    return entity.fields().stream()
        .filter(field -> field.precision() != null)
        .findFirst()
        .map(
            field ->
                new TooLarge(
                    field.name(),
                    "BigDecimal.TEN.pow("
                        + (field.precision() - (field.scale() == null ? 0 : field.scale()))
                        + ")"));
  }

  /**
   * The arguments of a DTO with the values of the DTO in the variable that a client gives, and none
   * that the service gives: neither the identifier nor the audit times.
   */
  public String writableArguments(String variable) {
    List<String> arguments = new ArrayList<>();
    arguments.add("null");
    for (FieldModel field : entity.fields()) {
      arguments.add(variable + "." + field.name() + "()");
    }
    for (RelationModel relation : entity.relations()) {
      arguments.add(variable + "." + relation.idField() + "()");
    }
    if (entity.audited()) {
      arguments.add("null");
      arguments.add("null");
    }
    return String.join(", ", arguments);
  }
}
