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
package me.yakar.tib.cli

import spock.lang.Specification

class RefusalExceptionSpec extends Specification {

  def "one problem is the message"() {
    when:
    RefusalException e = new RefusalException('x is already a tib project')

    then:
    e.message == 'x is already a tib project'
    e.problems() == ['x is already a tib project']
  }

  def "the advice follows the message on a line of its own, and is no problem"() {
    when:
    RefusalException e = new RefusalException('x has changes:\n   M A.java', [' M A.java'], 'Commit them.')

    then:
    e.message == "x has changes:\n   M A.java${System.lineSeparator()}Commit them."
    e.problems() == [' M A.java']
  }
}
