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

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The problems found in a spec, each with the path of the value it is about, as in {@code
 * entities[0].fields[2].length}. The structural rules come from the {@link Doc} annotations of the
 * records, the same ones the JSON Schema is generated from.
 */
final class Problems {

  private final List<String> messages = new ArrayList<>();

  void add(String path, String message) {
    messages.add(path + ": " + message);
  }

  boolean isEmpty() {
    return messages.isEmpty();
  }

  List<String> messages() {
    return List.copyOf(messages);
  }

  /** Checks a record and everything in it against the rules of its annotations. */
  void check(Object record, String path) {
    for (RecordComponent component : record.getClass().getRecordComponents()) {
      Doc doc = component.getAnnotation(Doc.class);
      String name = path.isEmpty() ? component.getName() : path + "." + component.getName();
      check(value(record, component), doc, name);
    }
  }

  private void check(Object value, Doc doc, String path) {
    switch (value) {
      case null -> {
        if (doc.required()) {
          add(path, "required");
        }
      }
      case String string -> {
        if (!doc.pattern().isEmpty() && !string.matches(doc.pattern())) {
          add(path, "'" + string + "' does not match " + doc.pattern());
        }
      }
      case Integer integer -> {
        if (doc.minimum() != Doc.NO_MINIMUM && integer < doc.minimum()) {
          add(path, integer + " is below " + doc.minimum());
        }
      }
      case List<?> list -> {
        for (int i = 0; i < list.size(); i++) {
          if (list.get(i) == null) {
            add(path + "[" + i + "]", "missing");
          } else {
            check(list.get(i), doc, path + "[" + i + "]");
          }
        }
      }
      default -> {
        if (value.getClass().isRecord()) {
          check(value, path);
        }
      }
    }
  }

  private static Object value(Object record, RecordComponent component) {
    try {
      return component.getAccessor().invoke(record);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("cannot read " + component.getName(), e);
    }
  }

  /** The list as the model keeps it: never null, never changed. */
  static <T> List<T> list(List<T> list) {
    return list == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
  }
}
