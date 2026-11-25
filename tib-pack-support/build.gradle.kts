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
// and their tests that the templates read, the rendering of a pack's precompiled templates, the
// templates that are the same for every stack, and the Gradle wrapper of a new service. Its test
// fixtures are the specifications that the tests of every pack extend.
import gg.jte.ContentType

plugins {
  `java-library`
  `java-test-fixtures`
  id("tib.java-conventions")
  id("gg.jte.gradle")
}

description = "tib pack support: template models and rendering, shared by the template packs"

dependencies {
  api(project(":tib-core"))
  api(libs.jte.runtime)
  implementation(libs.google.java.format)
  testFixturesApi(platform(libs.groovy.bom))
  testFixturesApi(libs.spock.core)
  // The fixtures check that the generated Java is in the layout of google-java-format.
  testFixturesImplementation(libs.google.java.format)
}

// The Gradle wrapper of this build, which a pack writes into a new service.
tasks.processResources {
  val root = layout.settingsDirectory
  val wrapper = "me/yakar/tib/pack/wrapper"
  from(root.files("gradlew", "gradlew.bat")) { into(wrapper) }
  from(
    root.files("gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"),
  ) {
    into("$wrapper/gradle/wrapper")
  }
}

// Coverage leaves out the jar of the Gradle wrapper.
tasks.withType<JacocoReportBase>().configureEach {
  classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude("**/*.jar") } })
}

// The templates under src/main/jte are the ones that every pack shares: the build of a pack
// compiles them with its own. The tests render some of theirs, which jte compiles into a directory
// of the build that only the tests have on their class path.
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
