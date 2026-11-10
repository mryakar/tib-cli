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

class ChangeSpec extends Specification {

  def "a change deletes its file when there is no content after it"() {
    expect:
    new Change('a.txt', 'a\n'.bytes, null, Owner.GENERATOR, false).deletes()
    !new Change('a.txt', null, 'a\n'.bytes, Owner.GENERATOR, false).deletes()
  }

  def "the diff of a file that is new or deleted has /dev/null on that side"() {
    expect:
    new Change('a.txt', before?.bytes, after?.bytes, Owner.GENERATOR, false).diff() == diff

    where:
    before | after | diff
    null   | 'a\n' | '--- /dev/null\n+++ b/a.txt\n@@ -0,0 +1 @@\n+a\n'
    'a\n'  | null  | '--- a/a.txt\n+++ /dev/null\n@@ -1 +0,0 @@\n-a\n'
    'a\n'  | 'b\n' | '--- a/a.txt\n+++ b/a.txt\n@@ -1 +1 @@\n-a\n+b\n'
  }

  def "the diff of a file that is not text says only that it differs"() {
    expect:
    Change.diff('f.bin', before as byte[], after as byte[]) == diff

    where:
    before             | after        | diff
    null               | [0xCA, 0xFE] | 'Binary files /dev/null and b/f.bin differ\n'
    [0x61, 0x00, 0x62] | null         | 'Binary files a/f.bin and /dev/null differ\n'
    [0x61]             | [0xFF]       | 'Binary files a/f.bin and b/f.bin differ\n'
  }

  def "content is text unless it has a zero byte or is not UTF-8"() {
    expect:
    Change.binary(content as byte[]) == binary

    where:
    content                                | binary
    null                                   | false
    []                                     | false
    'Şube\n'.getBytes('UTF-8') as List     | false
    [0x61, 0x00]                           | true
    'Şube\n'.getBytes('windows-1254') as List | true
  }
}
