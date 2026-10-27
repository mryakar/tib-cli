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

// The code is compiled for Java 21 and runs on 21 and later; the runtime that the packages embed
// may be newer.
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
      implementation(platform(libs.findLibrary("spock-bom").get()))
      runtimeOnly("org.junit.platform:junit-platform-launcher")
    }
  }
}

// Code is compiled for Java 21; -PtestJavaVersion runs the tests on a newer JVM, such as the one
// the packages embed.
val testJavaVersion = providers.gradleProperty("testJavaVersion").map(String::toInt).orElse(21)

tasks.withType<Test>().configureEach {
  javaLauncher = javaToolchains.launcherFor {
    languageVersion = testJavaVersion.map(JavaLanguageVersion::of)
  }
}

jacoco { toolVersion = version("jacoco") }

// Coverage counts every test suite. Taking the data from the tasks adds no dependency on them, so
// it is declared.
tasks.withType<JacocoReportBase>().configureEach {
  val tests = tasks.withType<Test>()
  dependsOn(tests)
  executionData(*tests.toTypedArray())
  // The classes that the build generates from the templates of a pack are the template engine's.
  classDirectories.setFrom(
    classDirectories.files.map { fileTree(it) { exclude("gg/jte/generated/**") } },
  )
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
