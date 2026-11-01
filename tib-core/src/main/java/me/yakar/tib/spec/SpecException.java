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
package me.yakar.tib.spec;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import me.yakar.tib.TibException;

/**
 * A spec that tib cannot use: it is not YAML, not a spec, or breaks a rule of the spec; or a spec
 * file that cannot be read.
 */
public class SpecException extends TibException {

  @Serial private static final long serialVersionUID = 1L;

  SpecException(List<String> problems) {
    super("spec", problems);
  }

  SpecException(String problem) {
    this(List.of(problem));
  }

  SpecException(Path file, IOException cause) {
    super("read", file, cause);
  }
}
