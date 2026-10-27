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
  sourceDirectory = layout.projectDirectory.dir("src/main/jte").asFile.toPath()
  contentType = ContentType.Plain
}

// The tests generate the example service of the documentation and compare it with its snapshot:
// the expected files, kept next to the tests rather than among the resources, which leave out
// files such as .gitignore. The acceptance test generates the example service and builds it with
// Gradle, against a database in a container where Docker runs Linux containers.
val examples = layout.settingsDirectory.dir("docs/modules/ROOT/examples")
val snapshots = layout.projectDirectory.dir("src/test/snapshots")
val windows = System.getProperty("os.name").startsWith("Windows")

tasks.test {
  inputs.dir(examples)
  inputs.dir(snapshots)
  systemProperty("tib.examples", examples.asFile.absolutePath)
  systemProperty("tib.snapshots", snapshots.asFile.absolutePath)
}

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
          // The output of the build of the service, so that a failure shows in the log of the
          // build.
          testLogging.showStandardStreams = true
          inputs.dir(examples)
          systemProperty("tib.examples", examples.asFile.absolutePath)
          // The service is built with the wrapper of this build, as a user would build it.
          val wrapper = if (windows) "gradlew.bat" else "gradlew"
          systemProperty("tib.gradlew", layout.settingsDirectory.file(wrapper).asFile.absolutePath)
        }
      }
    }
  }
}

tasks.check { dependsOn(testing.suites.named("acceptanceTest")) }
