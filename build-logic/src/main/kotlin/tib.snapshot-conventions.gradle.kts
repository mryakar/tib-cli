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
// The snapshots of what a module generates, under src/test/snapshots: its unit tests read them, and
// write what they generate under build/snapshots/test.
// ./gradlew test updateSnapshots --continue: the snapshots become what the unit tests wrote, and
// git diff shows the change. Copied by hand, as Gradle's copies leave out .gitignore files. The
// unit tests start with nothing under build/snapshots/test, so the snapshots become what this run
// of them wrote: all of them, when the run is the whole suite.
import me.yakar.tib.gradle.snapshots.TestRun

plugins { id("tib.java-conventions") }

tasks {
  // The tests run again when the snapshots are updated: a test restored from the build cache has no
  // .gitignore or .gitattributes among what it wrote, as Gradle's default excludes leave them out.
  val updating =
    gradle.startParameter.taskNames.any {
      it == "updateSnapshots" || it.endsWith(":updateSnapshots")
    }
  // A run of some of the unit tests writes only their snapshots, and the others would be deleted.
  val filtered =
    gradle.startParameter.taskNames.any { it == "--tests" || it.startsWith("--tests=") }
  val expected = layout.projectDirectory.dir("src/test/snapshots").asFile
  val actual = layout.buildDirectory.dir("snapshots/test")
  // Tests that did not run in this build wrote nothing to copy: updateSnapshots asks for a run.
  val testRun = gradle.sharedServices.registerIfAbsent("$path:testRun", TestRun::class) {}

  test {
    usesService(testRun)
    inputs.dir(expected).withPropertyName("snapshots").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(actual).withPropertyName("actualSnapshots")
    jvmArgumentProviders.add(
      CommandLineArgumentProvider {
        listOf(
          "-Dtib.snapshots=${expected.absolutePath}",
          "-Dtib.snapshots.actual=${actual.get().asFile.absolutePath}",
        )
      },
    )
    outputs.cacheIf("the snapshots are not being updated") { !updating }
    outputs.upToDateWhen { !updating }
    doFirst {
      actual.get().asFile.deleteRecursively()
      testRun.get().ran.set(true)
    }
  }

  register("updateSnapshots") {
    group = "verification"
    description =
      "Replaces the snapshots with what the unit tests wrote: run it after the whole suite."
    mustRunAfter(test)
    usesService(testRun)
    doLast {
      check(!filtered) {
        "updateSnapshots replaces every snapshot with what the unit tests of this run wrote:" +
          " run it without --tests"
      }
      check(testRun.get().ran.get()) {
        "The unit tests did not run in this build, so there is nothing of theirs to copy:" +
          " run them again with the full name of the task, as in test updateSnapshots --continue"
      }
      val written = actual.get().asFile
      check(written.isDirectory) { "$written is missing: the unit tests wrote no snapshot" }
      expected.deleteRecursively()
      written.copyRecursively(expected)
    }
  }
}
