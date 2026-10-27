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
// The engine: the intermediate representation, the input and generator contracts, the virtual
// file tree and the state file. It has no dependency on Spring.
plugins { id("tib.java-conventions") }

description = "tib engine: model, generator contracts, virtual file tree and state"

dependencies {
  implementation(platform(libs.jackson.bom))
  implementation(libs.jackson.databind)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.jsqlparser)
  // The tests check the spec file against the JSON Schema that the engine publishes.
  testImplementation(libs.json.schema.validator)
}

// The tests read the examples of the documentation, the spec and the DDL, so that they cannot
// drift apart.
val examples = layout.settingsDirectory.dir("docs/modules/ROOT/examples")

tasks.test {
  inputs.dir(examples)
  systemProperty("tib.examples", examples.asFile.absolutePath)
}
