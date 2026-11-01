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
// The default template pack: a plain Spring Boot 3 service with JPA, PostgreSQL, Flyway and
// Testcontainers. Its templates are compiled by the build, so that the packaged runtime needs no
// compiler.
import gg.jte.ContentType

plugins {
  id("tib.java-conventions")
  id("gg.jte.gradle")
}

description = "tib default template pack: Spring Boot 3, JPA, PostgreSQL, Flyway, Testcontainers"

dependencies {
  implementation(project(":tib-core"))
  implementation(libs.jte.runtime)
}

jte {
  generate()
  contentType = ContentType.Plain
}

// The versions of the generated stack come from the version catalog, where Dependabot sees them.
tasks.processResources {
  val stack =
    mapOf(
      "boot" to libs.versions.generated.spring.boot.get(),
      "postgres" to libs.versions.generated.postgres.get(),
    )
  inputs.properties(stack)
  filesMatching("**/pack.properties") { expand(stack) }
}

// Coverage leaves out the classes that jte generates from the templates.
tasks.withType<JacocoReportBase>().configureEach {
  classDirectories.setFrom(
    classDirectories.files.map { fileTree(it) { exclude("gg/jte/generated/**") } },
  )
}

// The tests compare the generated example service with its snapshot under src/test/snapshots
// (resources would leave out .gitignore); the acceptance test builds it with Gradle.
val windows = System.getProperty("os.name").startsWith("Windows")

testing {
  suites {
    register<JvmTestSuite>("acceptanceTest") {
      description = "Generates the example service and builds it with Gradle."
      dependencies {
        implementation(project())
        implementation(project(":tib-core"))
      }
      targets.all {
        testTask.configure {
          shouldRunAfter(tasks.test)
          // Shows the output of the service's build in the log.
          testLogging.showStandardStreams = true
          // The service is built with the wrapper of this build, as a user would build it.
          val wrapper = if (windows) "gradlew.bat" else "gradlew"
          systemProperty("tib.gradlew", layout.settingsDirectory.file(wrapper).asFile.absolutePath)
        }
      }
    }
  }
}

tasks.check { dependsOn(testing.suites.named("acceptanceTest")) }
