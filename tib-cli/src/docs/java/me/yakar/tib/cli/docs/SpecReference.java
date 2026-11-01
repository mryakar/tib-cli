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
package me.yakar.tib.cli.docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.yakar.tib.spec.SpecSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the properties of the spec file as AsciiDoc tables, one for each object of its JSON
 * Schema: the property, its type, its default and its description.
 */
public final class SpecReference {

  private static final String ROOT = "Spec";

  private SpecReference() {}

  public static void main(String[] args) throws IOException {
    Path file = Path.of(args[0]);
    Files.createDirectories(file.getParent());
    Files.writeString(file, adoc(JsonMapper.builder().build().readTree(SpecSchema.json())));
  }

  private static String adoc(JsonNode schema) {
    StringBuilder adoc =
        new StringBuilder("// Generated from the JSON Schema of the spec file by the build.\n");
    Set<String> written = new LinkedHashSet<>();
    Deque<String> objects = new ArrayDeque<>();
    objects.add(ROOT);
    while (!objects.isEmpty()) {
      String name = objects.remove();
      if (!written.add(name)) {
        continue;
      }
      JsonNode object = name.equals(ROOT) ? schema : schema.path("$defs").path(name);
      adoc.append("\n[[spec-").append(name).append("]]\n=== ").append(name).append("\n\n");
      adoc.append(object.path("description").asString()).append("\n\n");
      adoc.append("[cols=\"2,3,2,5\"]\n|===\n|Property |Type |Default |Description\n");
      Set<String> required =
          object.path("required").values().stream()
              .map(JsonNode::asString)
              .collect(Collectors.toSet());
      for (Map.Entry<String, JsonNode> property : object.path("properties").properties()) {
        JsonNode value = property.getValue();
        adoc.append("\n|`").append(property.getKey()).append("`\n");
        adoc.append('|').append(type(value, objects)).append('\n');
        adoc.append('|').append(defaultOf(required.contains(property.getKey()), value));
        adoc.append("\n|").append(value.path("description").asString()).append('\n');
      }
      adoc.append("|===\n");
    }
    return adoc.toString();
  }

  /** The type of a property; the objects it refers to join the queue. */
  private static String type(JsonNode property, Deque<String> objects) {
    if (property.has("$ref")) {
      String name = property.path("$ref").asString().replace("#/$defs/", "");
      objects.add(name);
      return "<<spec-" + name + "," + name + ">>";
    }
    if (property.has("enum")) {
      return "one of "
          + property.path("enum").values().stream()
              .map(value -> "`" + value.asString() + "`")
              .collect(Collectors.joining(", "));
    }
    String type = property.path("type").asString();
    if (type.equals("array")) {
      return "list of " + type(property.path("items"), objects);
    }
    JsonNode minimum = property.path("minimum");
    JsonNode maximum = property.path("maximum");
    if (minimum.isMissingNode()) {
      return type;
    }
    if (minimum.equals(maximum)) {
      return type + ", " + minimum.asLong();
    }
    if (maximum.isMissingNode()) {
      return type + ", at least " + minimum.asLong();
    }
    return type + ", " + minimum.asLong() + " to " + maximum.asLong();
  }

  private static String defaultOf(boolean required, JsonNode property) {
    if (required) {
      return "required";
    }
    return property.has("default") ? "`" + property.path("default").asString() + "`" : "";
  }
}
