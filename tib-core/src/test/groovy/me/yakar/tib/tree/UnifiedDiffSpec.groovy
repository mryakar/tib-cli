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
package me.yakar.tib.tree

import spock.lang.Specification

class UnifiedDiffSpec extends Specification {

  def "a new file is all additions"() {
    expect:
    UnifiedDiff.of('a/b.txt', null, 'one\ntwo\n') == '''\
--- /dev/null
+++ b/a/b.txt
@@ -0,0 +1,2 @@
+one
+two
'''
  }

  def "a changed line in the middle has three lines of context"() {
    given:
    String before = (1..10).collect { "line $it" }.join('\n') + '\n'
    String after = before.replace('line 5', 'five')

    expect:
    UnifiedDiff.of('f', before, after) == '''\
--- a/f
+++ b/f
@@ -2,7 +2,7 @@
 line 2
 line 3
 line 4
-line 5
+five
 line 6
 line 7
 line 8
'''
  }

  def "changes far apart are two hunks, close ones share a hunk"() {
    given:
    String before = (1..20).collect { "line $it" }.join('\n') + '\n'

    expect:
    UnifiedDiff.of('f', before, before.replace('line 2\n', 'two\n').replace('line 19\n', '')) == '''\
--- a/f
+++ b/f
@@ -1,5 +1,5 @@
 line 1
-line 2
+two
 line 3
 line 4
 line 5
@@ -16,5 +16,4 @@
 line 16
 line 17
 line 18
-line 19
 line 20
'''
    UnifiedDiff.of('f', before, before.replace('line 2\n', 'two\n').replace('line 8\n', 'line 8\neight\n')) == '''\
--- a/f
+++ b/f
@@ -1,11 +1,12 @@
 line 1
-line 2
+two
 line 3
 line 4
 line 5
 line 6
 line 7
 line 8
+eight
 line 9
 line 10
 line 11
'''
  }

  def "a last line without a newline is marked, on either side"() {
    expect:
    UnifiedDiff.of('f', 'a\nb', 'a\nb\n') == '''\
--- a/f
+++ b/f
@@ -1,2 +1,2 @@
 a
-b
\\ No newline at end of file
+b
'''
    UnifiedDiff.of('f', 'a\n', 'a\nb') == '''\
--- a/f
+++ b/f
@@ -1 +1,2 @@
 a
+b
\\ No newline at end of file
'''
  }

  def "emptying or filling a file is one hunk"() {
    expect:
    UnifiedDiff.of('f', 'a\n', '') == '--- a/f\n+++ b/f\n@@ -1 +0,0 @@\n-a\n'
    UnifiedDiff.of('f', '', 'a\n') == '--- a/f\n+++ b/f\n@@ -0,0 +1 @@\n+a\n'
  }

  def "the same content has no hunks"() {
    expect:
    UnifiedDiff.of('f', 'a\n', 'a\n') == '--- a/f\n+++ b/f\n'
  }

  def "the lines of #text are #lines"() {
    expect:
    UnifiedDiff.lines(text) == lines

    where:
    text       | lines
    null       | []
    ''         | []
    'a'        | ['a']
    'a\n'      | ['a\n']
    'a\n\n'    | ['a\n', '\n']
    '\n'       | ['\n']
    'a\nb'     | ['a\n', 'b']
  }

  def "the edit script prefers to keep the common lines"() {
    expect:
    UnifiedDiff.edits(['a', 'b', 'c'], ['b', 'c', 'd'])*.kind() == [
      UnifiedDiff.Edit.Kind.DELETE,
      UnifiedDiff.Edit.Kind.EQUAL,
      UnifiedDiff.Edit.Kind.EQUAL,
      UnifiedDiff.Edit.Kind.INSERT
    ]
    UnifiedDiff.edits(['a', 'x', 'b'], ['a', 'y', 'b'])*.line() == ['a', 'x', 'y', 'b']
    UnifiedDiff.edits([], ['a'])*.kind() == [UnifiedDiff.Edit.Kind.INSERT]
    UnifiedDiff.edits(['a'], [])*.kind() == [UnifiedDiff.Edit.Kind.DELETE]
  }

  def "the lines that both texts begin and end with change nothing of the edits: #seed"() {
    given: 'lines of a few kinds, so that many of them are the same'
    Random random = new Random(seed)
    List<String> before = (0..<random.nextInt(12)).collect { 'abc'[random.nextInt(3)] }
    List<String> after = (0..<random.nextInt(12)).collect { 'abc'[random.nextInt(3)] }

    expect: 'the edits of the table over the whole texts'
    UnifiedDiff.edits(before, after) == table(before, after)

    where:
    seed << (1..500)
  }

  def "a long text with a change in it takes a table of the lines that changed"() {
    given: 'a text whose table over all of its lines would not fit in memory'
    List<String> before = (1..200_000).collect { "line $it\n" as String }
    List<String> after = before.collect { it == 'line 100000\n' ? 'changed\n' : it }

    expect:
    UnifiedDiff.edits(before, after).findAll { it.kind() != UnifiedDiff.Edit.Kind.EQUAL } == [
      new UnifiedDiff.Edit(UnifiedDiff.Edit.Kind.DELETE, 'line 100000\n'),
      new UnifiedDiff.Edit(UnifiedDiff.Edit.Kind.INSERT, 'changed\n')
    ]
  }

  /** The edits by the table of longest common subsequences over all the lines. */
  private static List<UnifiedDiff.Edit> table(List<String> before, List<String> after) {
    int n = before.size()
    int m = after.size()
    int[][] common = new int[n + 1][m + 1]
    for (int i = n - 1; i >= 0; i--) {
      for (int j = m - 1; j >= 0; j--) {
        common[i][j] = before[i] == after[j] ? common[i + 1][j + 1] + 1 : Math.max(common[i + 1][j], common[i][j + 1])
      }
    }
    List<UnifiedDiff.Edit> edits = []
    int i = 0
    int j = 0
    while (i <n || j <m) {
      if (i <n && j <m && before[i] == after[j]) {
        edits << new UnifiedDiff.Edit(UnifiedDiff.Edit.Kind.EQUAL, before[i++])
        j++
      } else if (i <n && (j == m || common[i + 1][j] >= common[i][j + 1])) {
        edits << new UnifiedDiff.Edit(UnifiedDiff.Edit.Kind.DELETE, before[i++])
      } else {
        edits << new UnifiedDiff.Edit(UnifiedDiff.Edit.Kind.INSERT, after[j++])
      }
    }
    edits
  }
}
