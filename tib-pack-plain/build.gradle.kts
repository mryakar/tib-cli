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
// The plain template pack: a service in plain Java, without a framework of dependency injection,
// on the HTTP server of the JDK, with JDBC, PostgreSQL, Flyway, Jackson and Testcontainers.
plugins { id("tib.pack-conventions") }

description =
  "tib plain template pack: plain Java, the JDK's HTTP server, JDBC, PostgreSQL, Flyway, Jackson"

// The versions of the stack that the pack generates, from the version catalog, where Dependabot
// sees them.
pack {
  versions =
    mapOf(
      "jackson" to libs.versions.generated.jackson.get(),
      "pgjdbc" to libs.versions.generated.pgjdbc.get(),
      "flyway" to libs.versions.generated.flyway.get(),
      "junit" to libs.versions.generated.junit.get(),
      "mockito" to libs.versions.generated.mockito.get(),
      "testcontainers" to libs.versions.generated.testcontainers.get(),
      "archunit" to libs.versions.generated.archunit.get(),
      "postgres" to libs.versions.generated.postgres.get(),
      "googleJavaFormat" to libs.versions.google.java.format.get(),
    )
}
