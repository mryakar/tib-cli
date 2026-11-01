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
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

tasks.withType<JavaCompile>().configureEach {
  options.release = 21
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
val testJavaVersion = providers.gradleProperty("testJavaVersion").map(String::toInt).orElse(21)

// The tests read the examples of the documentation and, in a module that has them, the snapshots
// of what it generates; they write what they generate under build/snapshots/<task>.
tasks.withType<Test>().configureEach {
  javaLauncher = javaToolchains.launcherFor {
    languageVersion = testJavaVersion.map(JavaLanguageVersion::of)
  }
  val examples = layout.settingsDirectory.dir("docs/modules/ROOT/examples")
  val snapshots = layout.projectDirectory.dir("src/test/snapshots").takeIf { it.asFile.isDirectory }
  val actual = layout.buildDirectory.dir("snapshots/$name")
  inputs.dir(examples).withPropertyName("examples").withPathSensitivity(PathSensitivity.RELATIVE)
  snapshots?.let {
    inputs.dir(it).withPropertyName("snapshots").withPathSensitivity(PathSensitivity.RELATIVE)
  }
  outputs.dir(actual).withPropertyName("actualSnapshots")
  // As arguments, the absolute paths are no inputs, so the cached results fit any checkout.
  jvmArgumentProviders.add(
    CommandLineArgumentProvider {
      listOfNotNull(
        "-Dtib.examples=${examples.asFile.absolutePath}",
        snapshots?.let { "-Dtib.snapshots=${it.asFile.absolutePath}" },
        "-Dtib.snapshots.actual=${actual.get().asFile.absolutePath}",
      )
    },
  )
}

jacoco { toolVersion = version("jacoco") }

// Coverage counts every test suite; executionData(tasks) adds no task dependency, so dependsOn
// does.
tasks.withType<JacocoReportBase>().configureEach {
  val tests = tasks.withType<Test>()
  dependsOn(tests)
  executionData(*tests.toTypedArray())
}

tasks.jacocoTestReport { reports { xml.required = true } }

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
