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
package me.yakar.tib.pack.plain;

import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.pack.PackProperties;
import me.yakar.tib.pack.Templates;

/**
 * The plain pack: a service in plain Java, without a framework of dependency injection, its objects
 * wired by hand; the HTTP server of the JDK, JDBC on PostgreSQL, Flyway, Jackson and
 * Testcontainers, built with Gradle. It arranges a service in the anemic architecture, in DDD or in
 * the hexagonal one.
 */
public final class PlainPack implements Pack {

  /** The templates of the pack, which its build compiles into a package of their own. */
  static final Templates TEMPLATES = new Templates("gg.jte.generated.plain");

  private final PackProperties properties = PackProperties.of(PlainPack.class);

  @Override
  public String name() {
    return "plain";
  }

  @Override
  public String version() {
    return properties.get("version");
  }

  /** Plain Java, whatever the versions of the libraries of the generated service. */
  @Override
  public String stack() {
    return "Plain Java";
  }

  /** The Jackson version of the JSON of the generated service. */
  public String jacksonVersion() {
    return properties.get("jackson");
  }

  /** The version of the JDBC driver of PostgreSQL. */
  public String pgjdbcVersion() {
    return properties.get("pgjdbc");
  }

  /** The Flyway version that migrates the database of the generated service. */
  public String flywayVersion() {
    return properties.get("flyway");
  }

  /** The JUnit version of the tests of the generated service. */
  public String junitVersion() {
    return properties.get("junit");
  }

  /** The Mockito version of the unit tests of the generated service. */
  public String mockitoVersion() {
    return properties.get("mockito");
  }

  /** The Testcontainers version that starts PostgreSQL for the tests of the generated service. */
  public String testcontainersVersion() {
    return properties.get("testcontainers");
  }

  /** The ArchUnit version of the tests of the architecture of the generated service. */
  public String archunitVersion() {
    return properties.get("archunit");
  }

  /** The version of google-java-format, whose layout the generated Java has. */
  public String formatterVersion() {
    return properties.get("google-java-format");
  }

  /** The PostgreSQL major version that the tests of the generated service run against. */
  public String postgresVersion() {
    return properties.get("postgres");
  }

  @Override
  public Generator init() {
    return new InitGenerator(this);
  }

  @Override
  public Generator entity() {
    return new EntityGenerator();
  }
}
