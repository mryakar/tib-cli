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
package me.yakar.tib.state

import me.yakar.tib.spec.Entity
import me.yakar.tib.spec.Spec
import me.yakar.tib.tree.Owner
import spock.lang.Specification

class StateSpec extends Specification {

  State state = new State(
  new State.Tib('0.1.0', 1),
  new State.PackRef('default', '0.1.0'),
  new Spec(1, 'vehicle-service', null, null, [new Entity('Vehicle', null, null, null, null, null, null)]),
  [
    new State.File('AGENTS.md', Owner.GENERATOR, 'abc'),
    new State.File('build.gradle.kts', Owner.DEVELOPER, 'def')
  ])

  def "the state is YAML that tib reads back"() {
    when:
    String yaml = state.yaml()

    then:
    yaml.startsWith('tib:\n  version: 0.1.0\n  spec: 1\npack:\n  name: default\n  version: 0.1.0\nspec:\n  tib: 1\n  service: vehicle-service\n')
    yaml.contains('      type: Long\n      strategy: identity\n')
    yaml.contains('files:\n- path: AGENTS.md\n  owner: generator\n  sha256: abc\n')
    State.read(yaml) == state
  }
}
