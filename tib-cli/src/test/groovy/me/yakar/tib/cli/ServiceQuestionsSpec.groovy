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

import java.nio.file.Path
import me.yakar.tib.cli.Tib.Result
import me.yakar.tib.spec.Names
import me.yakar.tib.spec.Spec
import spock.lang.Specification
import spock.lang.TempDir

class ServiceQuestionsSpec extends Specification {

  static final String NAME = 'The name of the service, in lower case words joined with hyphens: '
  static final String PACK = 'Its template pack: default, plain [default]: '
  static final String JAVA = 'Its Java version: 21 or 25 [21]: '
  static final String ARCHITECTURE = 'Its architecture: anemic, ddd, hexagonal [anemic]: '
  static final String TESTS = 'The kinds of its tests, separated by commas, or none: unit, integration, api, archunit [unit,integration,api]: '
  static final String DOCKER = 'A Dockerfile: yes or no [no]: '

  /** The questions in their order: the one of the options on the command line. */
  static final String QUESTIONS = NAME + PACK + JAVA + ARCHITECTURE + TESTS + DOCKER

  static final String VERSION = System.getProperty('tib.version')

  @TempDir
  Path dir

  Tib tib

  def setup() {
    tib = new Tib(dir)
  }

  def "the answers make the spec of the service: #what"() {
    given:
    tib.answers = new ByteArrayInputStream(answers.getBytes('UTF-8'))

    when:
    Result result = tib.init()

    then:
    result.exitCode() == 0
    result.out() == "Wrote ${tib.state().files().size() + 1} files to ${tib.root}.\n"
    result.err() == QUESTIONS
    Spec spec = tib.state().spec()
    spec.service() == 'vehicle-service'
    spec.pack() == pack
    spec.java() == java
    spec.architecture().toString() == architecture
    spec.tests()*.toString() == tests
    spec.docker() == docker
    tib.state().inputs() == []

    where:
    what                                | answers                                                        | pack             | java | architecture | tests                          | docker
    'the defaults'                      | 'vehicle-service\n\n\n\n\n\n'                                  | 'default'        | 21   | 'anemic'     | ['unit', 'integration', 'api'] | false
    'answers of their own, with spaces' | ' vehicle-service \n plain \n25\n ddd \napi, unit, archunit\nY\n' | 'plain'          | 25   | 'ddd'        | ['api', 'unit', 'archunit']    | true
    'no tests, and what the defaults are' | 'vehicle-service\ndefault\n21\nanemic\nnone\nno\n'           | 'default'        | 21   | 'anemic'     | []                             | false
    'a pack with its version'           | "vehicle-service\nplain@$VERSION\n\nhexagonal\nunit\nyes\n"     | "plain@$VERSION" | 21   | 'hexagonal'  | ['unit']                       | true
  }

  def "an answer that cannot be read is asked again, with the reason: #what"() {
    given:
    tib.answers = new ByteArrayInputStream(answers.getBytes('UTF-8'))

    when:
    Result result = tib.init()

    then:
    result.exitCode() == 0
    result.err() == QUESTIONS.replace(question, question + reason + '\n' + question)
    tib.state().spec() == Spec.of('vehicle-service')

    where:
    what                                    | question     | answers                                       | reason
    'no name'                               | NAME         | '\nvehicle-service\n\n\n\n\n\n'               | "service: '' does not match ${Names.SERVICE}"
    'a name that is not one of a service'   | NAME         | 'Vehicle Service\nvehicle-service\n\n\n\n\n\n' | "service: 'Vehicle Service' does not match ${Names.SERVICE}"
    'a pack that is not there'              | PACK         | 'vehicle-service\nspring\n\n\n\n\n\n'         | "'spring' is not one of default, plain"
    'a Java that tib does not generate for' | JAVA         | 'vehicle-service\n\n17\n\n\n\n\n'             | 'java: 17 is not one of 21, 25'
    'a Java that is not a number'           | JAVA         | 'vehicle-service\n\nx\n\n\n\n\n'              | "'x' is not a number"
    'an architecture that tib does not know' | ARCHITECTURE | 'vehicle-service\n\n\nlayered\n\n\n\n'       | "'layered' is not one of anemic, ddd, hexagonal"
    'a kind of test that tib does not know' | TESTS        | 'vehicle-service\n\n\n\nunit,e2e\n\n\n'       | "'e2e' is not one of unit, integration, api, archunit"
    'a kind of test given twice'            | TESTS        | 'vehicle-service\n\n\n\nunit,unit\n\n\n'      | "tests[1]: 'unit' is already there"
    'neither yes nor no'                    | DOCKER       | 'vehicle-service\n\n\n\n\nmaybe\n\n'          | "'maybe' is not yes or no"
  }
}
