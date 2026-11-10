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
package me.yakar.tib.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Pack;

/**
 * The default pack: a plain Spring Boot 4 service with JPA, PostgreSQL, Flyway and Testcontainers,
 * built with Gradle on Java 21.
 */
public final class DefaultPack implements Pack {

  /** The root of the sources of the generated service. */
  static final String MAIN = "src/main/java/";

  /** The root of the tests of the generated service. */
  static final String TEST = "src/test/java/";

  private final Properties properties =
      load(DefaultPack.class.getResourceAsStream("pack.properties"));

  @Override
  public String name() {
    return properties.getProperty("name");
  }

  @Override
  public String version() {
    return properties.getProperty("version");
  }

  /** Spring Boot, by its major version, as in {@code Spring Boot 4}. */
  @Override
  public String stack() {
    String boot = bootVersion();
    return "Spring Boot " + boot.substring(0, boot.indexOf('.'));
  }

  /** The Spring Boot version of the generated service. */
  public String bootVersion() {
    return properties.getProperty("boot");
  }

  /** The Java version of the generated service. */
  public String javaVersion() {
    return properties.getProperty("java");
  }

  /** The PostgreSQL major version that the tests of the generated service run against. */
  public String postgresVersion() {
    return properties.getProperty("postgres");
  }

  @Override
  public Generator init() {
    return new InitGenerator(this);
  }

  @Override
  public Generator entity() {
    return new EntityGenerator();
  }

  static Properties load(InputStream stream) {
    Properties properties = new Properties();
    try (stream) {
      properties.load(stream);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return properties;
  }
}
