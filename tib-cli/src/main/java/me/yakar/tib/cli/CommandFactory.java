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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Builds the commands of tib with the version that they print and write into the state file, and
 * the answers to the questions that they ask.
 */
final class CommandFactory implements IFactory {

  private final TibVersion version;

  private final InputStream answers;

  /**
   * With the version that the build wrote into the application, and the answers to its questions
   * from standard input.
   */
  CommandFactory() {
    this(TibVersion.ofBuild(), System.in);
  }

  CommandFactory(TibVersion version, InputStream answers) {
    this.version = version;
    this.answers = answers;
  }

  @Override
  public <K> K create(Class<K> cls) throws Exception {
    if (cls == TibVersion.class) {
      return cls.cast(version);
    }
    if (cls == GenerationRun.class) {
      return cls.cast(new GenerationRun(version.version()));
    }
    if (cls == Questions.class) {
      return cls.cast(new Questions(new InputStreamReader(answers, Charset.defaultCharset())));
    }
    if (cls == ServiceDirectory.class) {
      return cls.cast(new ServiceDirectory(version.version()));
    }
    return CommandLine.defaultFactory().create(cls);
  }
}
