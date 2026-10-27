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

import java.util.ArrayList;
import java.util.List;

/**
 * The unified diff of one file, as {@code git diff} and {@code patch} read it: three lines of
 * context around every change, and a marker for a last line without a newline.
 */
final class UnifiedDiff {

  private static final int CONTEXT = 3;
  private static final String NO_NEWLINE = "\\ No newline at end of file\n";

  private UnifiedDiff() {}

  /** The diff of the file at the path, from its content before (null when new) to after. */
  static String of(String path, String before, String after) {
    List<Edit> edits = edits(lines(before), lines(after));
    StringBuilder diff = new StringBuilder();
    diff.append(before == null ? "--- /dev/null" : "--- a/" + path).append('\n');
    diff.append("+++ b/").append(path).append('\n');
    int first = nextChange(edits, 0);
    while (first < edits.size()) {
      int last = first;
      for (int next = nextChange(edits, last + 1);
          next < edits.size() && next - last <= 2 * CONTEXT + 1;
          next = nextChange(edits, next + 1)) {
        last = next;
      }
      int start = Math.max(first - CONTEXT, 0);
      int end = Math.min(last + CONTEXT + 1, edits.size());
      hunk(diff, edits, start, end);
      first = nextChange(edits, end);
    }
    return diff.toString();
  }

  private static int nextChange(List<Edit> edits, int from) {
    int i = from;
    while (i < edits.size() && edits.get(i).kind() == Edit.Kind.EQUAL) {
      i++;
    }
    return i;
  }

  private static void hunk(StringBuilder diff, List<Edit> edits, int start, int end) {
    int beforeStart = 0;
    int afterStart = 0;
    for (int i = 0; i < start; i++) {
      beforeStart += edits.get(i).kind() == Edit.Kind.INSERT ? 0 : 1;
      afterStart += edits.get(i).kind() == Edit.Kind.DELETE ? 0 : 1;
    }
    int beforeCount = 0;
    int afterCount = 0;
    StringBuilder body = new StringBuilder();
    for (int i = start; i < end; i++) {
      Edit edit = edits.get(i);
      beforeCount += edit.kind() == Edit.Kind.INSERT ? 0 : 1;
      afterCount += edit.kind() == Edit.Kind.DELETE ? 0 : 1;
      body.append(edit.kind().sign);
      if (edit.line().endsWith("\n")) {
        body.append(edit.line());
      } else {
        body.append(edit.line()).append('\n').append(NO_NEWLINE);
      }
    }
    diff.append("@@ -")
        .append(range(beforeStart, beforeCount))
        .append(" +")
        .append(range(afterStart, afterCount))
        .append(" @@\n")
        .append(body);
  }

  private static String range(int start, int count) {
    if (count == 0) {
      return start + ",0";
    }
    return count == 1 ? String.valueOf(start + 1) : (start + 1) + "," + count;
  }

  /** The lines of a text, each with its newline; the last one may lack it. */
  static List<String> lines(String text) {
    List<String> lines = new ArrayList<>();
    int start = 0;
    while (text != null && start < text.length()) {
      int end = text.indexOf('\n', start);
      end = end < 0 ? text.length() : end + 1;
      lines.add(text.substring(start, end));
      start = end;
    }
    return lines;
  }

  /**
   * The shortest edit script from one list of lines to the other, by longest common subsequence;
   * where a line is replaced, the old one goes before the new one.
   */
  static List<Edit> edits(List<String> before, List<String> after) {
    int n = before.size();
    int m = after.size();
    int[][] common = new int[n + 1][m + 1];
    for (int i = n - 1; i >= 0; i--) {
      for (int j = m - 1; j >= 0; j--) {
        common[i][j] =
            before.get(i).equals(after.get(j))
                ? common[i + 1][j + 1] + 1
                : Math.max(common[i + 1][j], common[i][j + 1]);
      }
    }
    List<Edit> edits = new ArrayList<>();
    int i = 0;
    int j = 0;
    while (i < n || j < m) {
      if (i < n && j < m && before.get(i).equals(after.get(j))) {
        edits.add(new Edit(Edit.Kind.EQUAL, before.get(i)));
        i++;
        j++;
      } else if (i < n && (j == m || common[i + 1][j] >= common[i][j + 1])) {
        edits.add(new Edit(Edit.Kind.DELETE, before.get(i)));
        i++;
      } else {
        edits.add(new Edit(Edit.Kind.INSERT, after.get(j)));
        j++;
      }
    }
    return edits;
  }

  /** One line of the edit script. */
  record Edit(Kind kind, String line) {
    enum Kind {
      EQUAL(' '),
      INSERT('+'),
      DELETE('-');

      final char sign;

      Kind(char sign) {
        this.sign = sign;
      }
    }
  }
}
