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
package me.yakar.tib.pack.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Pack;
import me.yakar.tib.pack.Templates;

/**
 * The default pack: a plain Spring Boot 4 service with JPA, PostgreSQL, Flyway and Testcontainers,
 * built with Gradle.
 */
public final class SpringPack implements Pack {

  /** The templates of the pack, which its build compiles into a package of their own. */
  static final Templates TEMPLATES = new Templates("gg.jte.generated.spring");

  private final Properties properties = load(stream("pack.properties"));

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

  /** The ArchUnit version of the tests of the architecture of the generated service. */
  public String archunitVersion() {
    return properties.getProperty("archunit");
  }

  /** The version of google-java-format, whose layout the generated Java has. */
  public String formatterVersion() {
    return properties.getProperty("google-java-format");
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

  /** A file that the pack carries, next to this class, as bytes. */
  static byte[] resource(String name) {
    return bytes(stream(name));
  }

  private static InputStream stream(String name) {
    InputStream stream = SpringPack.class.getResourceAsStream(name);
    if (stream == null) {
      throw new IllegalStateException("the pack does not carry " + name);
    }
    return stream;
  }

  static byte[] bytes(InputStream stream) {
    try (stream) {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
