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
// The command line: picocli commands wired as Spring beans, and the application that the packages
// ship. Its Spring Boot version is the CLI's own runtime, not the one of the projects it generates.
plugins { id("tib.java-conventions") }

description = "tib command line"

dependencies {
  implementation(platform(libs.spring.boot.dependencies))
  implementation(project(":tib-core"))
  implementation(libs.spring.boot.starter)
  implementation(libs.picocli)
  implementation(libs.picocli.spring.boot.starter)
  runtimeOnly(project(":tib-pack-default"))
}

// The version that tib --version prints comes from the build.
val projectVersion = version.toString()

tasks.processResources {
  val version = projectVersion
  inputs.property("version", version)
  filesMatching("application.properties") { expand("version" to version) }
}

// The application test starts the CLI in a JVM of its own and compares its output with the version
// of the build.
tasks.test { systemProperty("tib.version", projectVersion) }
