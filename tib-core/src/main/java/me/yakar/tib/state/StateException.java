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

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import me.yakar.tib.TibException;

/** A state file that tib cannot read, or a file that it cannot keep in the state. */
public class StateException extends TibException {

  @Serial private static final long serialVersionUID = 1L;

  StateException(List<String> problems) {
    super("state file", problems);
  }

  StateException(String message) {
    super(message);
  }

  StateException(String verb, Path file, IOException cause) {
    super(verb, file, cause);
  }
}
