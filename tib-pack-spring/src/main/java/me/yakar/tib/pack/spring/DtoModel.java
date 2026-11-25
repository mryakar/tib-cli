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

import java.util.ArrayList;
import java.util.List;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.FieldModel;
import me.yakar.tib.pack.RelationModel;

/**
 * The DTO of an entity as the templates of the Spring pack see it: a record whose components carry
 * the rules of the spec as annotations of Bean Validation.
 */
public final class DtoModel {

  private final EntityModel entity;

  public DtoModel(EntityModel entity) {
    this.entity = entity;
  }

  /**
   * The components of the record, the type and the name of each after its validation: the
   * identifier, every field, the identifier of the target of every relation, and the audit times of
   * an audited entity.
   */
  public String components() {
    List<String> components = new ArrayList<>();
    components.add(entity.idType() + " " + entity.idField());
    for (FieldModel field : entity.fields()) {
      components.add(validation(field) + field.javaType() + " " + field.name());
    }
    for (RelationModel relation : entity.relations()) {
      components.add(
          (relation.nullable() ? "" : "@NotNull ")
              + relation.target().idType()
              + " "
              + relation.idField());
    }
    if (entity.audited()) {
      components.add("Instant createdAt");
      components.add("Instant updatedAt");
    }
    return String.join(", ", components);
  }

  /** The validation annotations of the component of a field, in front of its type. */
  private static String validation(FieldModel field) {
    return (field.nullable() ? "" : "@NotNull ")
        + (field.length() == null ? "" : "@Size(max = " + field.length() + ") ");
  }
}
