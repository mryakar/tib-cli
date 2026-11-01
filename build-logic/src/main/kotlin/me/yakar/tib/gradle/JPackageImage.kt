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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Builds an application image with jpackage: the application's jars and a runtime of the JDK,
 * trimmed to the modules the application uses.
 */
abstract class JPackageImage : JPackage() {

  /** The directory with the application's jars. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val input: DirectoryProperty

  @get:Input abstract val mainJar: Property<String>

  @get:Input abstract val mainClass: Property<String>

  /** The modules of the JDK to put into the runtime, one per line. */
  @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val modules: RegularFileProperty

  /** The options of jlink, which builds the runtime. */
  @get:Input abstract val jlinkOptions: ListProperty<String>

  override fun modeArguments(): List<String> = buildList {
    val modules = modules.get().asFile.readLines().filter { it.isNotBlank() }
    addAll(listOf("--type", "app-image"))
    addAll(listOf("--input", input.get().asFile.absolutePath))
    addAll(listOf("--main-jar", mainJar.get()))
    addAll(listOf("--main-class", mainClass.get()))
    addAll(listOf("--add-modules", modules.joinToString(",")))
    if (jlinkOptions.get().isNotEmpty()) {
      addAll(listOf("--jlink-options", jlinkOptions.get().joinToString(" ")))
    }
  }
}
