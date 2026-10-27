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
package me.yakar.tib.generate

import me.yakar.tib.tree.Tree
import spock.lang.Specification

class PacksSpec extends Specification {

  Pack one = pack('one', '1.0.0')
  Pack two = pack('two', '2.0.0')

  def "finds a pack by its name, and by its version when the reference pins it"() {
    expect:
    Packs.find('two', [one, two]).is(two)
    Packs.find('two@2.0.0', [one, two]).is(two)
  }

  def "a pack that is not there: #reference"() {
    when:
    Packs.find(reference, [one, two])

    then:
    PackException e = thrown()
    e.message == "there is no pack '$reference'"

    where:
    reference << ['three', 'two@1.0.0']
  }

  def "finds the packs on the class path"() {
    expect:
    Packs.find('test') instanceof TestPack
    Packs.find('test@0') instanceof TestPack

    when:
    Packs.find('default')

    then:
    thrown(PackException)
  }

  static Pack pack(String name, String version) {
    new Pack() {
          String name() {
            name
          }
          String version() {
            version
          }
          Generator init() {
            { Project project, Tree tree -> } as Generator
          }
        }
  }
}
