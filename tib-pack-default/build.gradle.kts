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
// The default template pack: a plain Spring Boot 4 service with JPA, PostgreSQL, Flyway and
// Testcontainers. Its templates are compiled by the build, so that the packaged runtime needs no
// compiler.
import gg.jte.ContentType

plugins {
  id("tib.java-conventions")
  alias(libs.plugins.jte)
}

description = "tib default template pack: Spring Boot 4, JPA, PostgreSQL, Flyway, Testcontainers"

dependencies {
  implementation(project(":tib-core"))
  implementation(libs.jte.runtime)
  implementation(libs.google.java.format)
}

jte {
  generate()
  contentType = ContentType.Plain
  trimControlStructures = true
}

// The pack ships with tib and has its version. The versions of the generated stack come from the
// version catalog, where Dependabot sees them.
val tibVersion = version.toString()

tasks.processResources {
  val properties =
    mapOf(
      "version" to tibVersion,
      "boot" to libs.versions.generated.spring.boot.get(),
      "java" to libs.versions.generated.java.get(),
      "postgres" to libs.versions.generated.postgres.get(),
      "googleJavaFormat" to libs.versions.google.java.format.get(),
    )
  inputs.properties(properties)
  filesMatching("**/pack.properties") { expand(properties) }
  // The Gradle wrapper of this build, which the pack writes into a new service.
  val root = layout.settingsDirectory
  from(root.files("gradlew", "gradlew.bat")) { into("me/yakar/tib/pack/wrapper") }
  from(
    root.files("gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"),
  ) {
    into("me/yakar/tib/pack/wrapper/gradle/wrapper")
  }
}

// Coverage leaves out the classes that jte generates from the templates, and the jar of the Gradle
// wrapper that the pack carries.
tasks.withType<JacocoReportBase>().configureEach {
  classDirectories.setFrom(
    classDirectories.files.map { fileTree(it) { exclude("gg/jte/generated/**", "**/*.jar") } },
  )
}

// The tests compare the generated example service with its snapshot under src/test/snapshots
// (resources would leave out .gitignore); the acceptance test builds it with its own wrapper.
testing {
  suites {
    register<JvmTestSuite>("acceptanceTest") {
      description = "Generates the example services and builds them with Gradle."
      dependencies {
        implementation(project())
        implementation(project(":tib-core"))
      }
      // The corner spec, which the unit tests compare with its snapshot, is built here too.
      sources { resources.srcDir("src/test/resources") }
      targets.all {
        testTask.configure {
          shouldRunAfter(tasks.test)
          // Shows the output of the service's build in the log.
          testLogging.showStandardStreams = true
        }
      }
    }
  }
}

tasks.check { dependsOn(testing.suites.named("acceptanceTest")) }
