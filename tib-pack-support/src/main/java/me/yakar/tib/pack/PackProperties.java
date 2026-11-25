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

/**
 * What the build of a pack says of it in the file {@code pack.properties} next to its class: the
 * version of the pack and the versions of the stack that it generates.
 */
public final class PackProperties {

  private final Properties properties;

  private PackProperties(Properties properties) {
    this.properties = properties;
  }

  /** The properties of the pack of the class. */
  public static PackProperties of(Class<?> pack) {
    InputStream stream = pack.getResourceAsStream("pack.properties");
    if (stream == null) {
      throw new IllegalStateException(pack.getSimpleName() + " does not carry pack.properties");
    }
    return new PackProperties(load(stream));
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

  /** The value of a property, or null when the file has none of the name. */
  public String get(String name) {
    return properties.getProperty(name);
  }
}
