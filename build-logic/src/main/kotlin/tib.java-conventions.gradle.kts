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
// What every Java module of the build shares: the Java version and strict compiler warnings.
plugins { java }

// The code is compiled for Java 21 and runs on 21 and later; the runtime that the packages embed
// may be newer.
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

tasks.withType<JavaCompile>().configureEach {
  options.release = 21
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
