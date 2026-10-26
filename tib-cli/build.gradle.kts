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
import me.yakar.tib.build.JPackage
import me.yakar.tib.build.JdkModules

plugins {
  id("tib.java-conventions")
  application
}

description = "tib command line"

application {
  applicationName = "tib"
  mainClass = "me.yakar.tib.cli.TibApplication"
}

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

// Packaging. jpackage builds the installer of this platform (.deb on Linux, .msi on Windows) and
// an application image that the portable zip wraps; each embeds a runtime of JDK 25, trimmed to
// the modules the application uses. The code still runs on Java 21 and later.
val packagingJdk = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
val windows = System.getProperty("os.name").startsWith("Windows")
val platform = if (windows) "windows" else "linux"

val jpackageInput =
  tasks.register<Sync>("jpackageInput") {
    description = "Collects the jars that the packages ship."
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("jpackage/input"))
  }

val jdkModules =
  tasks.register<JdkModules>("jdkModules") {
    description = "Lists the modules of the JDK that the packaged runtime needs."
    javaLauncher = packagingJdk
    jars.from(jpackageInput.map { fileTree(it.destinationDir) })
    output = layout.buildDirectory.file("jpackage/modules.txt")
  }

fun jpackage(name: String, type: String, configure: JPackage.() -> Unit) =
  tasks.register<JPackage>(name) {
    javaLauncher = packagingJdk
    this.type = type
    applicationName = "tib"
    // Windows accepts only numbers in a version, so a snapshot packages as its release version.
    applicationVersion = projectVersion.substringBefore('-')
    input = layout.dir(jpackageInput.map { it.destinationDir })
    mainJar = tasks.jar.flatMap { it.archiveFileName }
    mainClass = application.mainClass
    modules = jdkModules.flatMap { it.output }
    options.addAll(
      "--vendor",
      "Amo",
      "--description",
      "The Idempotent Boilerplate: scaffolding CLI for Spring Boot services",
    )
    // The installers also tell where the application comes from.
    if (type != "app-image") {
      options.addAll("--about-url", "https://github.com/mryakar/tib-cli")
    }
    configure()
  }

val jpackageAppImage =
  jpackage("jpackageAppImage", "app-image") {
    description = "Builds the application image that the portable zip wraps."
    destination = layout.buildDirectory.dir("jpackage/app-image")
  }

val portableZip =
  tasks.register<Zip>("portableZip") {
    description =
      "Zips the application image: tib with its runtime, for a machine without admin rights."
    from(jpackageAppImage.map { it.destination })
    // The archive is reproducible, so it carries no permissions of its own; the launcher gets its.
    eachFile {
      if (relativePath.segments.contains("bin")) {
        permissions { unix("rwxr-xr-x") }
      }
    }
    archiveFileName = "tib-$projectVersion-$platform-x64.zip"
    destinationDirectory = layout.buildDirectory.dir("jpackage/zip")
  }

val installer =
  if (windows) {
    jpackage("jpackageMsi", "msi") {
      description = "Builds the Windows installer."
      resources = layout.projectDirectory.dir("src/jpackage/windows")
      destination = layout.buildDirectory.dir("jpackage/msi")
      // A fixed upgrade code, so that installing a newer version replaces the older one.
      options.addAll("--win-console", "--win-upgrade-uuid", "d91cba2b-e77e-47de-8bcc-4ca76321cfdf")
    }
  } else {
    jpackage("jpackageDeb", "deb") {
      description = "Builds the Debian package."
      resources = layout.projectDirectory.dir("src/jpackage/linux")
      destination = layout.buildDirectory.dir("jpackage/deb")
      options.addAll(
        "--linux-package-name",
        "tib",
        "--linux-deb-maintainer",
        "ahmet@yakar.me",
        "--linux-app-category",
        "utils",
      )
    }
  }

tasks.register<Sync>("packages") {
  description = "Builds the packages of this platform: the installer and the portable zip."
  group = "distribution"
  from(installer.map { it.destination })
  from(portableZip)
  into(layout.buildDirectory.dir("packages"))
}
