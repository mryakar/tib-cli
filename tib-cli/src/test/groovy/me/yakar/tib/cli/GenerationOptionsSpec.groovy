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

import me.yakar.tib.cli.Tib.Result
import spock.lang.Specification

class GenerationOptionsSpec extends Specification {

  def "a run writes, and refuses a service with uncommitted changes, unless the options say otherwise"() {
    given:
    GenerationOptions options = new GenerationOptions()

    expect:
    !options.dryRun()
    !options.allowDirty()
  }

  def "tib #command takes the options"() {
    when:
    Result result = new Tib().run(*command.split(' '), '--help')

    then:
    result.exitCode() == 0
    result.out().contains('--dry-run')
    result.out().contains('--allow-dirty')

    where:
    command << ['init service', 'generate entity']
  }
}
