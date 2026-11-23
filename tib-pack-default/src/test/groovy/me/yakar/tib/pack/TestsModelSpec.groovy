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

import me.yakar.tib.generate.Project
import me.yakar.tib.spec.SpecReader
import spock.lang.Specification

class TestsModelSpec extends Specification {

  def "the tests #tests are told of as they are"() {
    given:
    TestsModel model = new TestsModel(new Project(SpecReader.parse("tib: 1\nservice: s\ntests: $tests\n")))

    expect:
    model.any() == any
    model.container() == container
    model.archunit() == tests.contains('archunit')
    !any || model.entityTests() == words

    where:
    tests                       | any   | container | words
    '[unit, integration, api]'  | true  | true      | '`XRepositoryTests` and `XApiTests` against PostgreSQL in a container, and\n`XServiceTests` with the repository and the mapper mocked'
    '[api, integration]'        | true  | true      | '`XRepositoryTests` and `XApiTests` against PostgreSQL in a container'
    '[integration]'             | true  | true      | '`XRepositoryTests` against PostgreSQL in a container'
    '[api, unit]'               | true  | true      | '`XApiTests` against PostgreSQL in a container, and\n`XServiceTests` with the repository and the mapper mocked'
    '[unit]'                    | true  | false     | '`XServiceTests` with the repository and the mapper mocked'
    '[]'                        | false | false     | null
    '[archunit]'                | false | false     | null
    '[unit, archunit]'          | true  | false     | '`XServiceTests` with the repository and the mapper mocked'
  }
}
