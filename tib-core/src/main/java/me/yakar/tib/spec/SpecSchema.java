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
package me.yakar.tib.spec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON Schema of the spec file, generated from the records of the model and their {@link Doc}
 * annotations: what the schema accepts is what the reader accepts.
 */
public final class SpecSchema {

  // Two spaces, a line feed on every platform, one item per line, no space before a colon.
  private static final JsonMapper JSON =
      JsonMapper.builder()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .defaultPrettyPrinter(
              new DefaultPrettyPrinter(
                      Separators.createDefaultInstance()
                          .withObjectNameValueSpacing(Separators.Spacing.AFTER))
                  .withObjectIndenter(new DefaultIndenter("  ", "\n"))
                  .withArrayIndenter(new DefaultIndenter("  ", "\n")))
          .build();

  private SpecSchema() {}

  /** The schema as JSON, ending with a newline. */
  public static String json() {
    return JSON.writeValueAsString(schema()) + "\n";
  }

  /** The schema as the tree of its keywords. */
  static Map<String, Object> schema() {
    Map<String, Object> definitions = new LinkedHashMap<>();
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    schema.put("title", "tib spec");
    schema.putAll(object(Spec.class, definitions));
    schema.put("$defs", definitions);
    return schema;
  }

  /** The schema of one record of the model, without the records it refers to. */
  static Map<String, Object> of(Class<?> record) {
    return object(record, new LinkedHashMap<>());
  }

  private static Map<String, Object> object(Class<?> record, Map<String, Object> definitions) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("description", record.getAnnotation(Doc.class).value());
    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();
    for (RecordComponent component : record.getRecordComponents()) {
      Doc doc = component.getAnnotation(Doc.class);
      properties.put(component.getName(), property(component.getGenericType(), doc, definitions));
      if (doc.required()) {
        required.add(component.getName());
      }
    }
    schema.put("properties", properties);
    if (!required.isEmpty()) {
      schema.put("required", required);
    }
    schema.put("additionalProperties", false);
    return schema;
  }

  private static Map<String, Object> property(Type type, Doc doc, Map<String, Object> definitions) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("description", doc.value());
    schema.putAll(type(type, doc, definitions));
    if (!doc.defaultValue().isEmpty()) {
      schema.put(
          "default",
          type == Boolean.class ? Boolean.valueOf(doc.defaultValue()) : doc.defaultValue());
    }
    return schema;
  }

  private static Map<String, Object> type(Type type, Doc doc, Map<String, Object> definitions) {
    Map<String, Object> schema = new LinkedHashMap<>();
    if (type == String.class) {
      schema.put("type", "string");
      if (!doc.pattern().isEmpty()) {
        schema.put("pattern", doc.pattern());
      }
    } else if (type == Integer.class) {
      schema.put("type", "integer");
      if (doc.minimum() != Doc.NO_MINIMUM) {
        schema.put("minimum", doc.minimum());
      }
    } else if (type == Boolean.class) {
      schema.put("type", "boolean");
    } else if (type instanceof ParameterizedType list) {
      schema.put("type", "array");
      schema.put("items", type(list.getActualTypeArguments()[0], doc, definitions));
    } else if (((Class<?>) type).isEnum()) {
      schema.put("type", "string");
      schema.put(
          "enum",
          Arrays.stream(((Class<?>) type).getEnumConstants()).map(Object::toString).toList());
    } else {
      Class<?> record = (Class<?>) type;
      definitions.put(record.getSimpleName(), object(record, definitions));
      schema.put("$ref", "#/$defs/" + record.getSimpleName());
    }
    return schema;
  }
}
