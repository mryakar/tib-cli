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
package me.yakar.tib.generate;

import java.util.Arrays;
import me.yakar.tib.tree.Change;

/**
 * A file that lasts, and that tib leaves as it is, with what a run compares of it; the content
 * before or after is null when there is none.
 */
public record Lasting(String path, byte[] before, byte[] after) {

  /**
   * The unified diff from the content before to the content after; empty when they are the same.
   */
  public String diff() {
    return Arrays.equals(before, after) ? "" : Change.diff(path, before, after);
  }
}
