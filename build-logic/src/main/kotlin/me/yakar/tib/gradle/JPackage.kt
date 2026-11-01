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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations

/**
 * Runs the jpackage of a JDK: [JPackageImage] builds an application image, [JPackageInstaller] a
 * native package from one.
 */
abstract class JPackage : DefaultTask() {

  /** The JDK whose jpackage runs and whose runtime the package embeds. */
  @get:Nested abstract val javaLauncher: Property<JavaLauncher>

  @get:Input abstract val applicationName: Property<String>

  /** jpackage's --app-version; Windows accepts only numbers and dots. */
  @get:Input abstract val applicationVersion: Property<String>

  /** Further jpackage options, such as the ones of one platform. */
  @get:Input abstract val options: ListProperty<String>

  @get:OutputDirectory abstract val destination: DirectoryProperty

  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Inject protected abstract val fileSystemOperations: FileSystemOperations

  /** jpackage's --type and the options that build that type. */
  protected abstract fun modeArguments(): List<String>

  @TaskAction
  fun run() {
    val destination = destination.get().asFile
    fileSystemOperations.delete { delete(destination) }
    destination.mkdirs()
    val jdk = javaLauncher.get().metadata.installationPath
    execOperations.exec {
      executable(jdk.file("bin/jpackage").asFile)
      args(modeArguments())
      args("--name", applicationName.get())
      args("--app-version", applicationVersion.get())
      args("--dest", destination.absolutePath)
      args(options.get())
    }
  }
}
