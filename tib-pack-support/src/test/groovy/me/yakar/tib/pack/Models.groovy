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
package me.yakar.tib.pack

import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader

/** The models of the entities of a spec, as the tests write it. */
final class Models {

  private Models() {}

  /** The models of the entities of the spec of a service, by name. */
  static Map<String, EntityModel> of(String spec) {
    EntityModel.models(new Project(SpecReader.parse("tib: 1\nservice: s\n$spec")))
  }
}
