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
// What every Java module of the build shares: the Java version, strict compiler warnings, the
// format, Spock tests and full coverage.
plugins {
  java
  groovy
  jacoco
  id("com.diffplug.spotless")
}

val libs = versionCatalogs.named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

// Compiled for Java 21; runs on 21 and later.
val javaVersion = 21

java { toolchain { languageVersion = JavaLanguageVersion.of(javaVersion) } }

tasks.withType<JavaCompile>().configureEach {
  options.release = javaVersion
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

testing {
  suites.withType<JvmTestSuite>().configureEach {
    useSpock(version("spock"))
    dependencies {
      implementation(platform(libs.findLibrary("groovy-bom").get()))
      implementation(platform(libs.findLibrary("junit-bom").get()))
      runtimeOnly("org.junit.platform:junit-platform-launcher")
    }
  }
}

// -PtestJavaVersion runs the tests on another JVM, such as the one the packages embed.
val testJavaVersion =
  providers.gradleProperty("testJavaVersion").map(String::toInt).orElse(javaVersion)

// The tests read the examples of the documentation, and know the root and the version of the build.
// The unit tests of a module that has snapshots of what it generates also read them, and write what
// they generate under build/snapshots/test.
val tibVersion = version.toString()

tasks.withType<Test>().configureEach {
  javaLauncher = javaToolchains.launcherFor {
    languageVersion = testJavaVersion.map(JavaLanguageVersion::of)
  }
  // The tests generate, and the default pack formats what it generates with google-java-format.
  jvmArgs(me.yakar.tib.gradle.JavacExports.JVM_OPTIONS)
  systemProperty("tib.version", tibVersion)
  val root = layout.settingsDirectory
  val examples = root.dir("docs/modules/ROOT/examples")
  val snapshots =
    layout.projectDirectory.dir("src/test/snapshots").takeIf {
      name == "test" && it.asFile.isDirectory
    }
  val actual = snapshots?.let { layout.buildDirectory.dir("snapshots/$name") }
  inputs.dir(examples).withPropertyName("examples").withPathSensitivity(PathSensitivity.RELATIVE)
  snapshots?.let {
    inputs.dir(it).withPropertyName("snapshots").withPathSensitivity(PathSensitivity.RELATIVE)
  }
  actual?.let { outputs.dir(it).withPropertyName("actualSnapshots") }
  // As arguments, the absolute paths are no inputs, so the cached results fit any checkout.
  jvmArgumentProviders.add(
    CommandLineArgumentProvider {
      listOfNotNull(
        "-Dtib.root=${root.asFile.absolutePath}",
        "-Dtib.examples=${examples.asFile.absolutePath}",
        snapshots?.let { "-Dtib.snapshots=${it.asFile.absolutePath}" },
        actual?.let { "-Dtib.snapshots.actual=${it.get().asFile.absolutePath}" },
      )
    },
  )
}

// ./gradlew test updateSnapshots --continue: the snapshots become what the unit tests wrote, and
// git diff shows the change. Copied by hand, as Gradle's copies leave out .gitignore files. The
// unit tests start with nothing under build/snapshots/test, so the snapshots become what this run
// of them wrote: all of them, when the run is the whole suite.
val snapshotsDir = layout.projectDirectory.dir("src/test/snapshots").asFile

if (snapshotsDir.isDirectory) {
  // The tests run again when the snapshots are updated: a test restored from the build cache has no
  // .gitignore or .gitattributes among what it wrote, as Gradle's default excludes leave them out.
  val updating =
    gradle.startParameter.taskNames.any {
      it == "updateSnapshots" || it.endsWith(":updateSnapshots")
    }
  // A run of some of the unit tests writes only their snapshots, and the others would be deleted.
  val filtered =
    gradle.startParameter.taskNames.any { it == "--tests" || it.startsWith("--tests=") }
  val actual = layout.buildDirectory.dir("snapshots/test")
  tasks.test {
    outputs.cacheIf("the snapshots are not being updated") { !updating }
    outputs.upToDateWhen { !updating }
    doFirst { actual.get().asFile.deleteRecursively() }
  }
  tasks.register("updateSnapshots") {
    group = "verification"
    description =
      "Replaces the snapshots with what the unit tests wrote: run it after the whole suite."
    mustRunAfter(tasks.test)
    val expected = snapshotsDir
    doLast {
      check(!filtered) {
        "updateSnapshots replaces every snapshot with what the unit tests of this run wrote:" +
          " run it without --tests"
      }
      val written = actual.get().asFile
      check(written.isDirectory) { "$written is missing: run the unit tests first" }
      expected.deleteRecursively()
      written.copyRecursively(expected)
    }
  }
}

jacoco { toolVersion = version("jacoco") }

// Coverage counts the unit tests, which the report and the verification run first.
tasks.withType<JacocoReportBase>().configureEach { dependsOn(tasks.test) }

// Every instruction, branch and line of a module is covered, or the build fails.
tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      listOf("INSTRUCTION", "BRANCH", "LINE").forEach {
        limit {
          counter = it
          minimum = "1.0".toBigDecimal()
        }
      }
    }
  }
}

tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }

val licenseHeader = layout.settingsDirectory.file("gradle/spotless/license-header.txt")

spotless {
  java {
    target("src/*/java/**/*.java")
    googleJavaFormat(version("google-java-format"))
    licenseHeaderFile(licenseHeader, "(package|import|module|open|@)")
  }
  groovy {
    target("src/*/groovy/**/*.groovy")
    greclipse().configFile(layout.settingsDirectory.file("gradle/spotless/greclipse.properties"))
    licenseHeaderFile(licenseHeader, "(package|import)")
  }
}
