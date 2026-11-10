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

/** The JVM of the packaged launcher: its options, and the start-up cache that it loads. */
object Launcher {

  /** The name of the cache file, next to the jars; the launcher names it in its options. */
  fun cache(launcherName: String) = "$launcherName.aot"

  /**
   * The JVM options the launcher runs with; the cache is built with the same ones. The JVM says
   * nothing about the cache, which is missing until its script has run.
   */
  val JVM_OPTIONS =
    listOf("-XX:+UseCompactObjectHeaders", "-Xlog:aot=off,cds=off") + JavacExports.JVM_OPTIONS
}
