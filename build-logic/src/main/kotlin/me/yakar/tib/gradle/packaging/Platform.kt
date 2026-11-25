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
package me.yakar.tib.gradle.packaging

import java.nio.file.Path

/**
 * A platform that jpackage builds an application image for: its name in the names of the packages,
 * and where its image keeps the jars and the runtime. The paths are given from the directory that
 * jpackage writes the image to, with forward slashes.
 */
enum class Platform(
  val label: String,
  private val bundle: String,
  private val app: String,
  private val runtime: String,
) {
  LINUX("linux", "", "lib/app", "lib/runtime"),
  MACOS("macos", ".app", "Contents/app", "Contents/runtime/Contents/Home"),
  WINDOWS("windows", "", "app", "runtime");

  /** The image of a launcher: a directory of its name, on macOS an application bundle. */
  fun image(launcherName: String) = launcherName + bundle

  /** The directory of the jars. */
  fun appDirectory(launcherName: String) = "${image(launcherName)}/$app"

  /** The home of the runtime. */
  fun runtimeHome(launcherName: String) = "${image(launcherName)}/$runtime"

  /** The home of the runtime, from the directory of the jars. */
  val runtimeFromApp: String
    get() = Path.of(app).relativize(Path.of(runtime)).joinToString("/")

  companion object {

    /** The platform of the machine that builds. */
    fun current(): Platform {
      val os = System.getProperty("os.name")
      return when {
        os.startsWith("Windows") -> WINDOWS
        os.startsWith("Mac") -> MACOS
        else -> LINUX
      }
    }
  }
}
