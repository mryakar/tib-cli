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

import java.util.Arrays;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Reads a spec from YAML and checks it; a problem names the value it is about. */
public final class SpecReader {

  private static final YAMLMapper YAML =
      YAMLMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
          .build();

  private SpecReader() {}

  /** Reads a spec from its YAML. */
  public static Spec parse(String yaml) {
    return parse(yaml, Spec.class, "spec").check();
  }

  /**
   * Reads YAML into a type as a spec is read, unchecked: a property that the type does not know is
   * a problem, and a problem names the value it is about by its path from the root of the document,
   * as in {@code entities[0].fields[2]}.
   *
   * @param root the name of the document, for a problem with the whole of it
   * @throws SpecException when there is no document, it is not YAML, or it does not fit the type
   */
  public static <T> T parse(String yaml, Class<T> type, String root) {
    T value;
    // A file without a document has no first token, and one with an empty document is null.
    try (JsonParser parser = YAML.createParser(yaml)) {
      value = parser.nextToken() == null ? null : YAML.readValue(parser, type);
    } catch (DatabindException e) {
      throw new SpecException(path(e, root) + ": " + reason(e));
    } catch (JacksonException e) {
      throw new SpecException("not YAML: " + e.getOriginalMessage());
    }
    if (value == null) {
      throw new SpecException(root + ": the file is empty");
    }
    return value;
  }

  /** The path of the value the exception is about, as in {@code entities[0].fields[2]}. */
  private static String path(DatabindException e, String root) {
    StringBuilder path = new StringBuilder();
    for (JacksonException.Reference reference : e.getPath()) {
      if (reference.getPropertyName() != null) {
        if (!path.isEmpty()) {
          path.append('.');
        }
        path.append(reference.getPropertyName());
      } else {
        path.append('[').append(reference.getIndex()).append(']');
      }
    }
    return path.isEmpty() ? root : path.toString();
  }

  private static String reason(DatabindException e) {
    if (e instanceof UnrecognizedPropertyException unknown) {
      return "unknown property; the properties are "
          + unknown.getKnownPropertyIds().stream()
              .map(Object::toString)
              .sorted()
              .collect(Collectors.joining(", "));
    }
    if (e instanceof InvalidFormatException format && format.getTargetType().isEnum()) {
      return "'"
          + format.getValue()
          + "' is not one of "
          + Arrays.stream(format.getTargetType().getEnumConstants())
              .map(Object::toString)
              .collect(Collectors.joining(", "));
    }
    if (e instanceof InvalidFormatException format) {
      return "'" + format.getValue() + "' is not of type " + format.getTargetType().getSimpleName();
    }
    return e.getOriginalMessage();
  }
}
