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

import java.io.Serial;
import me.yakar.tib.TibException;

/**
 * A command that does not run, and why: the problem as a fragment, as in {@code x is already a tib
 * project: .tib/state.yaml is there}, then what to do about it, if anything, on a line of its own.
 */
final class RefusalException extends TibException {

  @Serial private static final long serialVersionUID = 1L;

  /** The problem, which is the message. */
  RefusalException(String problem) {
    super(problem);
  }

  /** The problem, with the advice on the line after it. */
  RefusalException(String problem, String advice) {
    this(problem + System.lineSeparator() + advice);
  }
}
