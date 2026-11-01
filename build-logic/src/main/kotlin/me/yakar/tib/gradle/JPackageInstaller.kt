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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** Builds a native package, such as a .deb or an .msi, from an application image with jpackage. */
abstract class JPackageInstaller : JPackage() {

  /** jpackage's --type: deb, msi and so on. */
  @get:Input abstract val type: Property<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val appImage: DirectoryProperty

  /** The license of the application. */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val licenseFile: RegularFileProperty

  /** Where the application comes from. */
  @get:Input abstract val aboutUrl: Property<String>

  /** jpackage's --resource-dir, with the files that replace its own templates. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val resources: DirectoryProperty

  override fun modeArguments(): List<String> = buildList {
    addAll(listOf("--type", type.get()))
    addAll(listOf("--app-image", appImage.get().asFile.absolutePath))
    addAll(listOf("--about-url", aboutUrl.get()))
    addAll(listOf("--license-file", licenseFile.get().asFile.absolutePath))
    if (resources.isPresent) {
      addAll(listOf("--resource-dir", resources.get().asFile.absolutePath))
    }
  }
}
