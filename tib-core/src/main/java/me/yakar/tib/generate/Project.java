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
package me.yakar.tib.generate;

import java.util.Locale;
import me.yakar.tib.spec.Spec;

/**
 * What the generators know about the project of a spec: its names, derived from the spec the same
 * way every time.
 */
public record Project(
    String name, String group, String basePackage, String applicationClass, Spec spec) {

  /** The project of the spec. */
  public static Project of(Spec spec) {
    String[] words = spec.service().split("-");
    StringBuilder application = new StringBuilder();
    for (String word : words) {
      application.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return new Project(
        spec.service(),
        spec.group(),
        spec.group() + "." + String.join("", words).toLowerCase(Locale.ROOT),
        application + "Application",
        spec);
  }

  /** The directory of the base package, relative to a source root. */
  public String packagePath() {
    return basePackage.replace('.', '/');
  }
}
