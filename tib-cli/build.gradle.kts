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
// The command line: the picocli commands, and the application that the packages ship.
import me.yakar.tib.gradle.JPackage
import me.yakar.tib.gradle.JPackageImage
import me.yakar.tib.gradle.JPackageInstaller
import me.yakar.tib.gradle.JdkModules
import me.yakar.tib.gradle.StartupCacheScript

plugins { id("tib.java-conventions") }

description = "tib command line"

dependencies {
  implementation(project(":tib-core"))
  implementation(libs.picocli)
  runtimeOnly(project(":tib-pack-default"))
}

// The version that tib --version prints comes from the build.
val projectVersion = version.toString()

tasks.processResources {
  val version = projectVersion
  inputs.property("version", version)
  filesMatching("**/version.properties") { expand("version" to version) }
}

// The application test starts the CLI in a JVM of its own and compares its output with the version
// of the build.
tasks.test { systemProperty("tib.version", projectVersion) }

// Packaging. jpackage builds the installer of this platform (.deb on Linux, .msi on Windows) and
// an application image that the portable zip wraps; each embeds a runtime of JDK 25, trimmed to
// the modules the application uses.
val packagingJdk = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
val windows = System.getProperty("os.name").startsWith("Windows")
val launcherName = "tib"
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

tasks.withType<JPackage>().configureEach {
  javaLauncher = packagingJdk
  applicationName = launcherName
  // A snapshot packages as its release version.
  applicationVersion = projectVersion.substringBefore('-')
  options.addAll(
    "--vendor",
    "Amo",
    "--description",
    "The Idempotent Boilerplate: a scaffolding CLI for Spring Boot services",
  )
}

val jpackageAppImage =
  tasks.register<JPackageImage>("jpackageAppImage") {
    description = "Builds the application image with jpackage."
    input = layout.dir(jpackageInput.map { it.destinationDir })
    mainJar = tasks.jar.flatMap { it.archiveFileName }
    mainClass = "me.yakar.tib.cli.TibApplication"
    modules = jdkModules.flatMap { it.output }
    destination = layout.buildDirectory.dir("jpackage/app-image")
    // The launcher is a console application; an option of the image, not of the installer.
    if (windows) {
      options.add("--win-console")
    }
    // jlink's defaults, without --strip-native-commands: the start-up cache is built with the
    // runtime's own java.
    jlinkOptions.addAll("--strip-debug", "--no-man-pages", "--no-header-files")
    // The launcher loads the start-up cache that the installers build (see StartupCacheScript),
    // silently when there is none, such as from the portable zip before its script was run.
    StartupCacheScript.JVM_OPTIONS.forEach { options.addAll("--java-options", it) }
    options.addAll(
      "--java-options",
      "-XX:AOTCache=\$APPDIR/${StartupCacheScript.cache(launcherName)}",
    )
  }

// The image that the packages ship: jpackage's, with the script that builds the start-up cache,
// without the runtime's commands other than java and, on Linux, without the java.desktop libraries
// that need X11, ALSA and FreeType.
val shippedImage =
  tasks.register<Sync>("shippedImage") {
    description = "Prepares the application image that the packages ship."
    from(jpackageAppImage.map { it.destination })
    if (windows) {
      exclude("$launcherName/runtime/bin/keytool.exe")
    } else {
      exclude(
        "$launcherName/lib/runtime/lib/libawt_xawt.so",
        "$launcherName/lib/runtime/lib/libsplashscreen.so",
        "$launcherName/lib/runtime/lib/libjsound.so",
        "$launcherName/lib/runtime/lib/libfontmanager.so",
        "$launcherName/lib/runtime/bin/keytool",
      )
    }
    into(layout.buildDirectory.dir("jpackage/image"))
    // The runtime's legal files are read-only, so a copy cannot overwrite the previous one.
    doFirst { destinationDir.deleteRecursively() }
    val launcher = launcherName
    val appDirectory = if (windows) "$launcher/app" else "$launcher/lib/app"
    val isWindows = windows
    doLast { StartupCacheScript.write(destinationDir.resolve(appDirectory), launcher, isWindows) }
  }

val portableZip =
  tasks.register<Zip>("portableZip") {
    description =
      "Zips the application image: tib with its runtime, for a machine without admin rights."
    from(shippedImage.map { it.destinationDir })
    // The archive is reproducible, so it carries no permissions of its own; the launcher, the
    // scripts, the runtime's commands and its jspawnhelper, which starts processes, get theirs.
    eachFile {
      if (relativePath.segments.contains("bin") || name.endsWith(".sh") || name == "jspawnhelper") {
        permissions { unix("rwxr-xr-x") }
      }
    }
    archiveFileName = "tib-$projectVersion-$platform-x64.zip"
    destinationDirectory = layout.buildDirectory.dir("jpackage/zip")
  }

// The installers are built from the image that the packages ship.
tasks.withType<JPackageInstaller>().configureEach {
  appImage = layout.dir(shippedImage.map { it.destinationDir.resolve(launcherName) })
  aboutUrl = "https://github.com/mryakar/tib-cli"
  licenseFile = layout.settingsDirectory.file("LICENSE")
}

val installer =
  if (windows) {
    tasks.register<JPackageInstaller>("jpackageMsi") {
      description = "Builds the Windows installer."
      type = "msi"
      resources = layout.projectDirectory.dir("src/jpackage/windows")
      destination = layout.buildDirectory.dir("jpackage/msi")
      // A fixed upgrade code, so that installing a newer version replaces the older one.
      options.addAll("--win-upgrade-uuid", "d91cba2b-e77e-47de-8bcc-4ca76321cfdf")
    }
  } else {
    tasks.register<JPackageInstaller>("jpackageDeb") {
      description = "Builds the Debian package."
      type = "deb"
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
// the man page that picocli-codegen writes, and the properties of the spec file and its schema from
// the records of the spec. Antora runs antoraContent for each version of the site.
val docs =
  sourceSets.create("docs") {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
  }

dependencies { "docsImplementation"(libs.picocli.codegen) }

val commandReference =
  tasks.register<JavaExec>("commandReference") {
    description = "Generates the command reference of the documentation from the commands."
    group = "documentation"
    classpath = docs.runtimeClasspath
    mainClass = "picocli.codegen.docgen.manpage.ManPageGenerator"
    val pages = layout.buildDirectory.dir("docs/commands")
    outputs.dir(pages)
    argumentProviders.add(
      CommandLineArgumentProvider {
        listOf(
          "--outdir=${pages.get().asFile.absolutePath}",
          "--factory=me.yakar.tib.cli.CommandFactory",
          "--force",
          "me.yakar.tib.cli.TibCommand",
        )
      },
    )
    // The man pages link to each other by file name; Antora resolves such a link from the root
    // of the pages, so the links get the directory of the reference pages. The heading of the exit
    // codes loses the colon of the help text.
    doLast {
      pages.get().asFile.listFiles()!!.forEach { page ->
        page.writeText(
          page
            .readText()
            .replace("xref:tib-", "xref:./tib-")
            .replace("== Exit codes:", "== Exit codes"),
        )
      }
    }
  }

// A task that runs a class of the documentation, which writes one file of the site.
fun docsFile(name: String, text: String, mainClass: String, path: String) =
  tasks.register<JavaExec>(name) {
    description = text
    group = "documentation"
    classpath = docs.runtimeClasspath
    this.mainClass = "me.yakar.tib.cli.docs.$mainClass"
    val file = layout.buildDirectory.file(path)
    outputs.file(file)
    argumentProviders.add(CommandLineArgumentProvider { listOf(file.get().asFile.absolutePath) })
  }

val schemaAttachment =
  docsFile(
    "schemaAttachment",
    "Writes the JSON Schema of the spec file for the documentation site.",
    "SchemaAttachment",
    "docs/schema/tib-spec.schema.json",
  )

val specReference =
  docsFile(
    "specReference",
    "Writes the properties of the spec file as tables for the documentation site.",
    "SpecReference",
    "docs/spec/spec-properties.adoc",
  )

tasks.register<Sync>("antoraContent") {
  description = "Assembles the generated files of the documentation in an Antora module."
  group = "documentation"
  from(commandReference) { into("modules/ROOT/partials/commands") }
  from(specReference) { into("modules/ROOT/partials") }
  from(schemaAttachment) { into("modules/ROOT/attachments") }
  into(layout.buildDirectory.dir("docs/antora"))
}
