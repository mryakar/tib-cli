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

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.util.Map;

/** The templates of the pack, compiled by the build. */
final class Templates {

  private static final TemplateEngine ENGINE = TemplateEngine.createPrecompiled(ContentType.Plain);

  private Templates() {}

  /** The text of the template with the parameters, by name. */
  static String render(String template, Map<String, Object> parameters) {
    StringOutput output = new StringOutput();
    ENGINE.render(template, parameters, output);
    return output.toString();
  }
}
