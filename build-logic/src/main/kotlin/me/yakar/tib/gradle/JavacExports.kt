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

/**
 * The packages of the JDK's compiler that google-java-format reads, which the default pack runs on
 * the Java it generates: every JVM that generates, the launcher's and the tests', opens them to the
 * class path.
 */
object JavacExports {

  /** The JVM options, one for each package. */
  val JVM_OPTIONS =
    listOf("api", "code", "file", "parser", "tree", "util").map {
      "--add-exports=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED"
    }
}
