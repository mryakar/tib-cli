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
package me.yakar.tib.ddl;

import java.io.Serial;
import java.util.List;

/** The DDL has problems that keep it from being read into a spec. */
public class DdlException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  // An immutable list, which serializes; the lint cannot see that.
  @SuppressWarnings("serial")
  private final List<String> problems;

  public DdlException(List<String> problems) {
    super(message(problems));
    this.problems = List.copyOf(problems);
  }

  /** The problems, each with the table or the column it is about. */
  public List<String> problems() {
    return problems;
  }

  private static String message(List<String> problems) {
    if (problems.size() == 1) {
      return "the DDL has a problem: " + problems.get(0);
    }
    return "the DDL has " + problems.size() + " problems:\n  " + String.join("\n  ", problems);
  }
}
