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
package me.yakar.tib.gradle

import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations

/**
 * Lists the modules of the JDK that the jars of an application use, with jdeps, so that the runtime
 * that jpackage embeds carries only those. The list goes to a file, one module per line.
 */
abstract class JdkModules : DefaultTask() {

  /** The JDK whose jdeps runs: the one whose runtime the packages embed. */
  @get:Nested abstract val javaLauncher: Property<JavaLauncher>

  @get:Classpath abstract val jars: ConfigurableFileCollection

  @get:OutputFile abstract val output: RegularFileProperty

  @get:Inject protected abstract val archiveOperations: ArchiveOperations

  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Inject protected abstract val fileSystemOperations: FileSystemOperations

  @TaskAction
  fun list() {
    val jdk = javaLauncher.get().metadata.installationPath
    val version = javaLauncher.get().metadata.languageVersion.asInt()
    // jdeps reads the classes of all the jars from one directory, as the class path they form.
    // Given the jars themselves, it would treat each one that has a module descriptor as a module
    // and stop when the descriptor requires a module it cannot find on the class path.
    val classes = temporaryDir.resolve("classes")
    fileSystemOperations.delete { delete(classes) }
    fileSystemOperations.copy {
      jars.forEach { from(archiveOperations.zipTree(it)) }
      // Only the classes matter: descriptors and the jars' metadata stay out.
      include("**/*.class")
      exclude("**/module-info.class")
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
      into(classes)
    }
    val out = ByteArrayOutputStream()
    execOperations.exec {
      executable(jdk.file("bin/jdeps").asFile)
      args("--ignore-missing-deps", "--multi-release", version.toString())
      args("--print-module-deps", classes.absolutePath)
      standardOutput = out
    }
    val modules = out.toString().lines().last { it.isNotBlank() }.split(',').map { it.trim() }
    output.get().asFile.writeText(modules.joinToString("\n", postfix = "\n"))
  }
}
