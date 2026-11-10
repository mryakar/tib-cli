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
package me.yakar.tib.state;

import java.util.Locale;

/**
 * A file that tib read the spec from or wrote, and that is no longer as tib read or wrote it.
 *
 * @param path the path of the file in the project, or that of an input as the state keeps it
 */
public record Difference(String path, Kind kind) {

  /** How the file differs from what tib read or wrote. */
  public enum Kind {
    /** An input was edited after tib read it; the next run of tib with it reads it again. */
    CHANGED,
    /** An input is not where tib read it; tib does not need it. */
    ABSENT,
    /** A file of the generator's was edited; git checkout, or the next run of tib, restores it. */
    EDITED,
    /** A file of the generator's, or a shared file, is not there; tib needs it. */
    MISSING,
    /** A file of the developer's was removed; tib does not write it again. */
    REMOVED;

    /** Whether the difference is a problem: tib needs the file as it read or wrote it. */
    public boolean problem() {
      return this != ABSENT && this != REMOVED;
    }

    /** The name in the output of tib. */
    @Override
    public String toString() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
