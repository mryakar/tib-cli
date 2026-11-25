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
import java.util.List;
import me.yakar.tib.tree.ProjectFiles;

/**
 * The Gradle wrapper of a new service, as the build of tib has it: the files of Gradle's, without a
 * header of tib's.
 */
public final class GradleWrapper {

  /** The files of the wrapper, relative to the project. */
  public static final List<String> FILES =
      List.of(
          "gradlew",
          "gradlew.bat",
          "gradle/wrapper/gradle-wrapper.jar",
          "gradle/wrapper/gradle-wrapper.properties");

  private GradleWrapper() {}

  /**
   * Stages the wrapper, every file of the developer's and written once; {@code gradlew} is written
   * as a file that can be run.
   */
  public static void write(ProjectFiles files) {
    for (String file : FILES) {
      files.once(file, bytes(file), file.equals("gradlew"));
    }
  }

  /** A file of the wrapper, which the build of tib put next to this class. */
  static byte[] bytes(String file) {
    InputStream stream = GradleWrapper.class.getResourceAsStream("wrapper/" + file);
    if (stream == null) {
      throw new IllegalStateException("tib does not carry the file " + file + " of the wrapper");
    }
    return bytes(stream);
  }

  static byte[] bytes(InputStream stream) {
    try (stream) {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
