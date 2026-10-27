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
package me.yakar.tib.spec

import spock.lang.Specification

class ProblemsSpec extends Specification {

  def "a value without constraints is accepted whatever it is"() {
    given:
    Problems problems = new Problems()

    when:
    problems.check(new Plain(-5, 'any text at all'), '')

    then:
    problems.empty
    problems.messages() == []
  }

  def "a record whose accessor fails cannot be checked"() {
    when:
    new Problems().check(new Broken('x'), '')

    then:
    IllegalStateException e = thrown()
    e.message == 'cannot read value'
    e.cause.cause instanceof UnsupportedOperationException
  }

  def "the list of the model is never null and never changes"() {
    expect:
    Problems.list(null) == []

    when:
    Problems.list(['a']).add('b')

    then:
    thrown(UnsupportedOperationException)
  }
}
