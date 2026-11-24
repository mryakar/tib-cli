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
// What a template pack shares whatever stack it generates: the models of a service, its entities
// and their tests that the templates read, and the rendering of a pack's precompiled templates.
import gg.jte.ContentType

plugins {
  `java-library`
  id("tib.java-conventions")
  alias(libs.plugins.jte)
}

description = "tib pack support: template models and rendering, shared by the template packs"

dependencies {
  api(project(":tib-core"))
  api(libs.jte.runtime)
  implementation(libs.google.java.format)
}

// The module has no templates of its own. The tests render some of theirs, which jte compiles
// into a directory of the build that only the tests have on their class path.
jte {
  precompile()
  sourceDirectory = layout.projectDirectory.dir("src/test/jte").asFile.toPath()
  targetDirectory = layout.buildDirectory.dir("jte-classes").map { it.asFile.toPath() }
  packageName = "gg.jte.generated.support"
  contentType = ContentType.Plain
  trimControlStructures = true
  // jte compiles them with the JDK that runs Gradle, for the Java of the tests.
  compileArgs = arrayOf("--release", "21")
}

sourceSets.test {
  runtimeClasspath +=
    files(layout.buildDirectory.dir("jte-classes")) { builtBy(tasks.precompileJte) }
}
