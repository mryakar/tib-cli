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
package me.yakar.tib.cli;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/** Builds the commands of tib with the version that they print and write into the state file. */
public final class CommandFactory implements IFactory {

  private final TibVersion version;

  /** With the version that the build wrote into the application. */
  public CommandFactory() {
    this(TibVersion.ofBuild());
  }

  CommandFactory(TibVersion version) {
    this.version = version;
  }

  @Override
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == TibVersion.class) {
      return cls.cast(version);
    }
    if (cls == GenerationOptions.class) {
      return cls.cast(new GenerationOptions(version));
    }
    if (cls == ServiceDirectory.class) {
      return cls.cast(new ServiceDirectory(version));
    }
    return CommandLine.defaultFactory().create(cls);
  }
}
