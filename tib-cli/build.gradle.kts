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
import me.yakar.tib.gradle.JPackage
import me.yakar.tib.gradle.JdkModules

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
    // The installers also tell where the application comes from and under which license.
    if (type != "app-image") {
      options.addAll("--about-url", "https://github.com/mryakar/tib-cli")
      options.addAll("--license-file", layout.settingsDirectory.file("LICENSE").asFile.absolutePath)
      inputs.file(layout.settingsDirectory.file("LICENSE"))
    }
    configure()
  }

val jpackageAppImage =
  jpackage("jpackageAppImage", "app-image") {
    description = "Builds the application image with jpackage."
    destination = layout.buildDirectory.dir("jpackage/app-image")
  }

// The image that the packages ship: jpackage's, on Linux without the libraries of java.desktop
// that would make the package depend on X11, ALSA and FreeType. Spring needs java.beans from that
// module, nothing that calls those libraries.
val shippedImage =
  tasks.register<Sync>("shippedImage") {
    description = "Prepares the application image that the packages ship."
    from(jpackageAppImage.map { it.destination })
    if (!windows) {
      exclude(
        "tib/lib/runtime/lib/libawt_xawt.so",
        "tib/lib/runtime/lib/libsplashscreen.so",
        "tib/lib/runtime/lib/libjsound.so",
        "tib/lib/runtime/lib/libfontmanager.so",
      )
    }
    into(layout.buildDirectory.dir("jpackage/image"))
    // The runtime's legal files are read-only, so a copy cannot overwrite the previous one.
    doFirst { destinationDir.deleteRecursively() }
  }

val portableZip =
  tasks.register<Zip>("portableZip") {
    description =
      "Zips the application image: tib with its runtime, for a machine without admin rights."
    from(shippedImage.map { it.destinationDir })
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
      appImage = layout.dir(shippedImage.map { it.destinationDir.resolve("tib") })
      resources = layout.projectDirectory.dir("src/jpackage/windows")
      destination = layout.buildDirectory.dir("jpackage/msi")
      // A fixed upgrade code, so that installing a newer version replaces the older one.
      options.addAll("--win-console", "--win-upgrade-uuid", "d91cba2b-e77e-47de-8bcc-4ca76321cfdf")
    }
  } else {
    jpackage("jpackageDeb", "deb") {
      description = "Builds the Debian package."
      appImage = layout.dir(shippedImage.map { it.destinationDir.resolve("tib") })
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

// Documentation. The command reference of the site is generated from the commands themselves, as
// the man page that picocli-codegen writes, into a content root of the Antora site under build/.
val docs =
  sourceSets.create("docs") {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
  }

dependencies { "docsImplementation"(libs.picocli.codegen) }

val antoraContent = layout.buildDirectory.dir("docs/antora")

val commandReference =
  tasks.register<JavaExec>("commandReference") {
    description = "Generates the command reference of the documentation from the commands."
    group = "documentation"
    classpath = docs.runtimeClasspath
    mainClass = "picocli.codegen.docgen.manpage.ManPageGenerator"
    val partials = antoraContent.map { it.dir("modules/ROOT/partials/commands") }
    outputs.dir(partials)
    argumentProviders.add(
      CommandLineArgumentProvider {
        listOf(
          "--outdir=${partials.get().asFile.absolutePath}",
          "--factory=me.yakar.tib.cli.docs.CommandFactory",
          "--force",
          "me.yakar.tib.cli.TibCommand",
        )
      },
    )
  }

tasks.register("antoraContent") {
  description = "Prepares the generated content root of the documentation site."
  group = "documentation"
  dependsOn(commandReference)
  // The content root needs a component descriptor; its version comes from the playbook.
  val descriptor = antoraContent.map { it.file("antora.yml") }
  outputs.file(descriptor)
  doLast { descriptor.get().asFile.writeText("name: tib\n") }
}
