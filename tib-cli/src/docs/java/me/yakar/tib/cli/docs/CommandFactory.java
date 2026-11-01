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
package me.yakar.tib.cli.docs;

import me.yakar.tib.cli.GenerationOptions;
import me.yakar.tib.cli.TibVersion;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Builds the commands without Spring, for the documentation generator and the tests: what the
 * commands take from the application gets a version of its own.
 */
public final class CommandFactory implements IFactory {

  private final String version;

  /** With the version {@code docs}, for the documentation generator. */
  public CommandFactory() {
    this("docs");
  }

  public CommandFactory(String version) {
    this.version = version;
  }

  @Override
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == TibVersion.class) {
      return cls.cast(new TibVersion(version));
    }
    if (cls == GenerationOptions.class) {
      return cls.cast(new GenerationOptions(new TibVersion(version)));
    }
    return CommandLine.defaultFactory().create(cls);
  }
}
