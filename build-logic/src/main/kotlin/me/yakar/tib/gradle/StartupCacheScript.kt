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

import java.io.File

/**
 * Writes, next to the jars of a jpackage application image, the script that builds the start-up
 * cache of the application: the AOT cache of the JDK, which the launcher loads on every run.
 *
 * The cache is bound to the class path it was built with, so the script builds it where the
 * application is installed, with the class path that the launcher's configuration names, in the
 * same order. The installers run the script when they install the application.
 *
 * The script records a training run and then assembles the cache from it, in two steps: the JDK's
 * one-step `-XX:AOTCacheOutput` relaunches the JVM through `JAVA_TOOL_OPTIONS`, which prints a
 * "Picked up" line that would end up in the output of the installation.
 */
object StartupCacheScript {

  /** The name of the cache file, next to the jars; the launcher names it in its options. */
  const val CACHE = "tib.aot"

  /** The JVM options the launcher runs with; the cache is built with the same ones. */
  val JVM_OPTIONS = listOf("-XX:+UseCompactObjectHeaders")

  /** Writes the script for one platform and returns it. */
  fun write(appDirectory: File, launcherName: String, windows: Boolean): File {
    val configuration = appDirectory.resolve("$launcherName.cfg").readLines()
    val mainClass =
      configuration.single { it.startsWith("app.mainclass=") }.substringAfter('=').trim()
    val jars =
      configuration
        .filter { it.startsWith("app.classpath=") }
        .map { it.substringAfter('=').trim().removePrefix("\$APPDIR").trimStart('/', '\\') }
    val script =
      if (windows) {
        appDirectory.resolve("startup-cache.cmd").apply {
          writeText(
            """
            |@echo off
            |rem Builds the start-up cache of $launcherName next to its jars. The installer runs it;
            |rem run it yourself after unpacking the portable zip.
            |set "app=%~dp0"
            |set "app=%app:~0,-1%"
            |set "java=%app%\..\runtime\bin\java.exe"
            |set "cp=${jars.joinToString(";") { "%app%\\$it" }}"
            |"%java%" ${JVM_OPTIONS.joinToString(" ")} -Xlog:aot=off,cds=off ^
            |  -XX:AOTMode=record -XX:AOTConfiguration="%app%\$CACHE.config" ^
            |  -cp "%cp%" $mainClass --version >nul || exit /b 1
            |"%java%" ${JVM_OPTIONS.joinToString(" ")} -Xlog:aot=off,cds=off ^
            |  -XX:AOTMode=create -XX:AOTConfiguration="%app%\$CACHE.config" -XX:AOTCache="%app%\$CACHE" ^
            |  -cp "%cp%" >nul
            |set "result=%errorlevel%"
            |del /q "%app%\$CACHE.config" 2>nul
            |exit /b %result%
            |"""
              .trimMargin()
              .replace("\n", "\r\n"),
          )
        }
      } else {
        appDirectory.resolve("startup-cache.sh").apply {
          writeText(
            """
            |#!/bin/sh
            |# Builds the start-up cache of $launcherName next to its jars. The package's installation
            |# runs it; run it yourself after unpacking the portable zip.
            |set -e
            |app=$(cd "$(dirname "$0")" && pwd)
            |java="${'$'}app/../runtime/bin/java"
            |cp="${jars.joinToString(":") { "\$app/$it" }}"
            |trap 'rm -f "${'$'}app/$CACHE.config"' EXIT
            |"${'$'}java" ${JVM_OPTIONS.joinToString(" ")} -Xlog:aot=off,cds=off \
            |  -XX:AOTMode=record -XX:AOTConfiguration="${'$'}app/$CACHE.config" \
            |  -cp "${'$'}cp" $mainClass --version >/dev/null
            |"${'$'}java" ${JVM_OPTIONS.joinToString(" ")} -Xlog:aot=off,cds=off \
            |  -XX:AOTMode=create -XX:AOTConfiguration="${'$'}app/$CACHE.config" -XX:AOTCache="${'$'}app/$CACHE" \
            |  -cp "${'$'}cp" >/dev/null
            |"""
              .trimMargin(),
          )
          setExecutable(true, false)
        }
      }
    return script
  }
}
