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

  def "a new file"() {
    expect:
    UnifiedDiff.of('a/b.txt', null, 'one\ntwo\n') == '''\
--- /dev/null
+++ b/a/b.txt
@@ -0,0 +1,2 @@
+one
+two
'''
  }

  def "a changed line in the middle, with three lines of context"() {
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

  def "a file emptied, and an empty file filled"() {
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
}
