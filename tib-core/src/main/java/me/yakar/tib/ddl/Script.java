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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An SQL script split into its statements, much as psql splits it: a semicolon ends a statement
 * unless it is in a string, a quoted name, a dollar-quoted body or a comment. A backslash between
 * two statements starts a command of psql, such as the {@code \restrict} that pg_dump writes, which
 * runs to the end of its line and is no statement.
 */
final class Script {

  /** The tag of a dollar-quoted body, as in {@code $$} or {@code $body$}. */
  private static final Pattern DOLLAR = Pattern.compile("\\$(?:[\\p{L}_][\\p{L}\\p{N}_]*)?\\$");

  private final String sql;
  private final List<Statement> statements = new ArrayList<>();

  /** The line of the character at {@link #counted}, from 1. */
  private int line = 1;

  /** The index of the first character of that line. */
  private int lineStart;

  /** The index up to which the lines are counted. */
  private int counted;

  private Script(String sql) {
    this.sql = sql;
  }

  /** A statement without its semicolon, with the line and the column where it starts, from 1. */
  record Statement(String text, int line, int column) {

    /**
     * Where in the script a line and a column of the statement are, both from 1: {@code line 3,
     * column 17}.
     */
    String at(int line, int column) {
      return "line "
          + (this.line + line - 1)
          + ", column "
          + (line == 1 ? this.column + column - 1 : column);
    }
  }

  /** The statements of the script, without those that are blank. */
  static List<Statement> split(String sql) {
    Script script = new Script(sql);
    script.split();
    return List.copyOf(script.statements);
  }

  private void split() {
    int length = sql.length();
    int start = -1;
    int i = 0;
    while (i < length) {
      char c = sql.charAt(i);
      if (Character.isWhitespace(c)) {
        i++;
      } else if (c == '\\' && start < 0 || sql.startsWith("--", i)) {
        i = endOfLine(i);
      } else if (sql.startsWith("/*", i)) {
        i = afterComment(i);
      } else if (c == ';') {
        add(start, i);
        start = -1;
        i++;
      } else {
        if (start < 0) {
          start = i;
        }
        i = next(c, i);
      }
    }
    add(start, length);
  }

  /** The index after the character at the index, or after the string or body that it opens. */
  private int next(char c, int i) {
    if (c == '\'') {
      return afterQuote(i, '\'', escapes(i));
    }
    if (c == '"') {
      return afterQuote(i, '"', false);
    }
    if (c == '$' && !(i > 0 && isNamePart(sql.charAt(i - 1)))) {
      Matcher tag = DOLLAR.matcher(sql).region(i, sql.length());
      if (tag.lookingAt()) {
        return after(tag.end(), tag.group());
      }
    }
    return i + 1;
  }

  /** Whether the string that opens at the index takes escapes: {@code E'...'}. */
  private boolean escapes(int quote) {
    return quote > 0
        && Character.toUpperCase(sql.charAt(quote - 1)) == 'E'
        && !(quote > 1 && isNamePart(sql.charAt(quote - 2)));
  }

  private static boolean isNamePart(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }

  /** The index after the quote that closes the one at the index; a doubled quote is in the text. */
  private int afterQuote(int open, char quote, boolean escapes) {
    int i = open + 1;
    while (i < sql.length()) {
      char c = sql.charAt(i);
      if (escapes && c == '\\') {
        i += 2;
      } else if (c != quote) {
        i++;
      } else if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
        i += 2;
      } else {
        return i + 1;
      }
    }
    return sql.length();
  }

  /**
   * The index after the end of the block comment that opens at the index; a comment in it has an
   * end of its own.
   */
  private int afterComment(int open) {
    int depth = 0;
    int i = open;
    while (i < sql.length()) {
      if (sql.startsWith("/*", i)) {
        depth++;
        i += 2;
      } else if (!sql.startsWith("*/", i)) {
        i++;
      } else if (--depth == 0) {
        return i + 2;
      } else {
        i += 2;
      }
    }
    return sql.length();
  }

  /** The index after the end that comes first from the index, or the length when there is none. */
  private int after(int from, String end) {
    int at = sql.indexOf(end, from);
    return at < 0 ? sql.length() : at + end.length();
  }

  private int endOfLine(int from) {
    int at = sql.indexOf('\n', from);
    return at < 0 ? sql.length() : at;
  }

  /** Adds the statement from the start to the end, unless there is none. */
  private void add(int start, int end) {
    if (start < 0) {
      return;
    }
    for (; counted < start; counted++) {
      if (sql.charAt(counted) == '\n') {
        line++;
        lineStart = counted + 1;
      }
    }
    statements.add(
        new Statement(sql.substring(start, end).stripTrailing(), line, start - lineStart + 1));
  }
}
