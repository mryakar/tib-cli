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

import me.yakar.tib.spec.Names;
import me.yakar.tib.spec.Spec;

/**
 * What the generators know about the project of a spec: its names, derived from the spec the same
 * way every time.
 */
public record Project(Spec spec) {

  /** The name of the service, as in {@code vehicle-service}. */
  public String name() {
    return spec.service();
  }

  /** The group of the service, as in {@code com.example}. */
  public String group() {
    return spec.group();
  }

  /**
   * The base package: the group and the name without hyphens, as in {@code
   * com.example.vehicleservice}.
   */
  public String basePackage() {
    return spec.group() + "." + spec.service().replace("-", "");
  }

  /** The class that starts the service, as in {@code VehicleServiceApplication}. */
  public String applicationClass() {
    return Names.type(spec.service().replace('-', '_')) + "Application";
  }

  /** The directory of the base package, relative to a source root. */
  public String packagePath() {
    return basePackage().replace('.', '/');
  }
}
