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

class FieldModelSpec extends Specification {

  def "a sample BigDecimal fits the precision and the scale of its column: #precision, #scale"() {
    given:
    FieldModel field = field("{ name: amount, type: BigDecimal$size }")

    expect:
    [field.sample(1), field.sample(2)] == samples.collect { 'new BigDecimal("' + it + '")' }

    where:
    precision | scale | size                          | samples
    null      | null  | ''                            | ['1', '2']
    12        | 2     | ', precision: 12, scale: 2'   | ['1.00', '2.00']
    null      | 2     | ', scale: 2'                  | ['1.00', '2.00']
    2         | 2     | ', precision: 2, scale: 2'    | ['0.01', '0.02']
  }

  /** The model of the field of an entity of a spec. */
  private static FieldModel field(String field) {
    EntityGenerator.models(new Project(SpecReader.parse("tib: 1\nservice: s\nentities:\n  - name: A\n    fields: [$field]\n"))).A.fields()[0]
  }
}
