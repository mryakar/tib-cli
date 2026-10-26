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

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations

/**
 * Runs the jpackage of a JDK: an application image, or a native package such as a .deb or an .msi,
 * with a runtime of that JDK trimmed to the modules the application uses.
 */
abstract class JPackage : DefaultTask() {

  /** The JDK whose jpackage runs and whose runtime the package embeds. */
  @get:Nested abstract val javaLauncher: Property<JavaLauncher>

  /** jpackage's --type: app-image, deb, msi and so on. */
  @get:Input abstract val type: Property<String>

  @get:Input abstract val applicationName: Property<String>

  /** jpackage's --app-version; Windows accepts only numbers and dots. */
  @get:Input abstract val applicationVersion: Property<String>

  /** The directory with the application's jars, when the task builds an application image. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val input: DirectoryProperty

  @get:Input @get:Optional abstract val mainJar: Property<String>

  @get:Input @get:Optional abstract val mainClass: Property<String>

  /** The modules of the JDK to put into the runtime, one per line. */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  @get:Optional
  abstract val modules: RegularFileProperty

  /** An application image to build the package from, instead of the jars and a new runtime. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val appImage: DirectoryProperty

  /** jpackage's --resource-dir, with the files that replace its own templates. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val resources: DirectoryProperty

  /** Further jpackage options, such as the ones of one platform. */
  @get:Input abstract val options: ListProperty<String>

  @get:OutputDirectory abstract val destination: DirectoryProperty

  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Inject protected abstract val fileSystemOperations: FileSystemOperations

  @TaskAction
  fun run() {
    val destination = destination.get().asFile
    fileSystemOperations.delete { delete(destination) }
    destination.mkdirs()
    val jdk = javaLauncher.get().metadata.installationPath
    execOperations.exec {
      executable(jdk.file("bin/jpackage").asFile)
      args("--type", type.get())
      args("--name", applicationName.get())
      args("--app-version", applicationVersion.get())
      if (appImage.isPresent) {
        args("--app-image", appImage.get().asFile.absolutePath)
      } else {
        val modules = modules.get().asFile.readLines().filter { it.isNotBlank() }
        args("--input", input.get().asFile.absolutePath)
        args("--main-jar", mainJar.get())
        args("--main-class", mainClass.get())
        args("--add-modules", modules.joinToString(","))
      }
      args("--dest", destination.absolutePath)
      if (resources.isPresent) {
        args("--resource-dir", resources.get().asFile.absolutePath)
      }
      args(options.get())
    }
  }
}
