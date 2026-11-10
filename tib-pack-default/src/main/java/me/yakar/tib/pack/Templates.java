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

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.util.Map;

/** The templates of the pack, compiled by the build. */
final class Templates {

  private static final TemplateEngine ENGINE = TemplateEngine.createPrecompiled(ContentType.Plain);

  private static final Formatter FORMATTER = new Formatter();

  private Templates() {}

  /**
   * The text of the file at the path: the header that says who owns it, as a comment of the file's
   * kind, then the template with the parameters, by name. Java is in the layout of
   * google-java-format, its imports sorted and the unused ones left out.
   */
  static String render(
      String path, String header, String template, Map<String, Object> parameters) {
    StringOutput output = new StringOutput();
    ENGINE.render(template, parameters, output);
    return format(path, comment(path, header) + "\n" + output);
  }

  /** The text of the file at the path, formatted if it is Java. */
  static String format(String path, String text) {
    if (!path.endsWith(".java")) {
      return text;
    }
    try {
      return FORMATTER.formatSourceAndFixImports(text);
    } catch (FormatterException e) {
      throw new IllegalStateException(path + " is not Java that google-java-format reads", e);
    }
  }

  /** The text as a comment of one line in the file at the path, by its extension. */
  static String comment(String path, String text) {
    return switch (path.substring(path.lastIndexOf('.') + 1)) {
      case "java", "kts" -> "// " + text;
      case "yaml", "gitignore", "gitattributes" -> "# " + text;
      case "sql" -> "-- " + text;
      case "md" -> "<!-- " + text + " -->";
      default -> throw new IllegalArgumentException("there is no comment in " + path);
    };
  }
}
