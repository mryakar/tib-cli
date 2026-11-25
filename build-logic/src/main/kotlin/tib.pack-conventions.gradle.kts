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
// What every template pack shares: templates that the build compiles, not tib when it runs, the
// versions of its stack in pack.properties, snapshots of the services it generates, and an
// acceptance test that builds them. The module of a pack is tib-pack-<stack>, and the stack names
// its packages.
import gg.jte.ContentType
import me.yakar.tib.gradle.pack.PackExtension

plugins {
  id("tib.java-conventions")
  id("tib.snapshot-conventions")
  id("gg.jte.gradle")
}

val stack = name.removePrefix("tib-pack-")
val pack = extensions.create<PackExtension>("pack")

dependencies {
  implementation(project(":tib-pack-support"))
  // The specification that the one of every pack extends.
  testImplementation(testFixtures(project(":tib-pack-support")))
}

// The templates of a pack are its own and the ones that every pack shares, which tib-pack-support
// holds. A template has one name among them.
val templates =
  tasks.register<Sync>("templates") {
    description = "Collects the templates of the pack and the ones that every pack shares."
    from(layout.settingsDirectory.dir("tib-pack-support/src/main/jte"))
    from(layout.projectDirectory.dir("src/main/jte"))
    into(layout.buildDirectory.dir("templates"))
    duplicatesStrategy = DuplicatesStrategy.FAIL
  }

// The package of the compiled templates is the pack's own: another pack has templates of the same
// names.
jte {
  generate()
  sourceDirectory = templates.map { it.destinationDir.toPath() }
  packageName = "gg.jte.generated.$stack"
  contentType = ContentType.Plain
  trimControlStructures = true
}

tasks.generateJte { dependsOn(templates) }

// The pack ships with tib and has its version.
tasks.processResources {
  val tibVersion = project.version.toString()
  val properties = pack.versions.map { it + ("version" to tibVersion) }
  inputs.property("pack", properties)
  filesMatching("**/pack.properties") { expand(properties.get()) }
}

// Coverage leaves out the classes that jte generates from the templates.
tasks.withType<JacocoReportBase>().configureEach {
  classDirectories.setFrom(
    classDirectories.files.map { fileTree(it) { exclude("gg/jte/generated/**") } },
  )
}

// The acceptance test generates the example services and builds each with its own wrapper.
testing {
  suites {
    register<JvmTestSuite>("acceptanceTest") {
      description = "Generates the example services and builds them with Gradle."
      dependencies {
        implementation(project())
        implementation(project(":tib-core"))
        // The specification that the acceptance test of every pack extends.
        implementation(testFixtures(project(":tib-pack-support")))
      }
      targets.all {
        testTask.configure {
          shouldRunAfter(tasks.test)
          // A generated service compiles with the JDK of its Java, which its build finds among the
          // JDKs of this one: one for every Java that a spec may give.
          val jdks =
            listOf(21, 25).map { java ->
              javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(java) }
            }
          jvmArgumentProviders.add(
            CommandLineArgumentProvider {
              listOf(
                "-Dtib.jdks=" +
                  jdks.joinToString(",") { it.get().metadata.installationPath.asFile.absolutePath },
              )
            },
          )
          // Shows the output of the service's build in the log.
          testLogging.showStandardStreams = true
        }
      }
    }
  }
}

tasks.check { dependsOn(testing.suites.named("acceptanceTest")) }
