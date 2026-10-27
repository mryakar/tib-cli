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
package me.yakar.tib.tree;

import java.util.Locale;

/** Who owns a generated file: what a run of tib may do to it. */
public enum Owner {
  /** The generator: the file is written on every run and is not edited by hand. */
  GENERATOR,
  /** The developer: the file is written once, when it is not there, and is theirs from then on. */
  DEVELOPER,
  /** Shared: the file is edited in place, never replaced. */
  SHARED;

  /** The name in the state file. */
  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
