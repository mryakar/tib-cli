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

class TemplatesSpec extends Specification {

  def "a header is a comment of the file's kind: #path"() {
    expect:
    Templates.comment(path, 'Mine.') == comment

    where:
    path                     | comment
    'src/main/java/A.java'   | '// Mine.'
    'build.gradle.kts'       | '// Mine.'
    'application.yaml'       | '# Mine.'
    '.gitignore'             | '# Mine.'
    'db/migration/V1__a.sql' | '-- Mine.'
    'AGENTS.md'              | '<!-- Mine. -->'
  }

  def "a file of a kind without comments gets no header"() {
    when:
    Templates.comment('logo.png', 'Mine.')

    then:
    IllegalArgumentException e = thrown()
    e.message == 'there is no comment in logo.png'
  }
}
