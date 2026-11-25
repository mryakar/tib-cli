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
package me.yakar.tib.pack

import spock.lang.Specification

class SourcesSpec extends Specification {

  def "the file of a class is under the sources or the tests, in the directory of its package"() {
    expect:
    Sources.main('a.b', 'C') == 'src/main/java/a/b/C.java'
    Sources.test('a.b', 'CTests') == 'src/test/java/a/b/CTests.java'
  }

  def "a type is imported into a file of another package"() {
    expect:
    Sources.importOf('a.b', 'a.b', 'C') == ''
    Sources.importOf('a', 'a.b', 'C') == 'import a.b.C;'
  }
}
