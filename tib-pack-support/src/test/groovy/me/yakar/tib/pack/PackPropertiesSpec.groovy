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

class PackPropertiesSpec extends Specification {

  def "the properties of a pack are the ones in the file next to its class"() {
    given:
    PackProperties properties = PackProperties.of(PackPropertiesSpec)

    expect:
    properties.get('version') == '1.2.3'
    properties.get('none') == null
  }

  def "a pack without the file is a mistake of its build, named by its class"() {
    when:
    PackProperties.of(String)

    then:
    IllegalStateException e = thrown()
    e.message == 'String does not carry pack.properties'
  }

  def "the file has to be readable"() {
    when:
    PackProperties.load(new InputStream() {
          int read() {
            throw new IOException('unreadable')
          }
        })

    then:
    thrown(UncheckedIOException)
  }
}
