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

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.cli.Tib.Result
import me.yakar.tib.state.State
import spock.lang.Specification
import spock.lang.TempDir

class ServiceDirectorySpec extends Specification {

  /** The commands that work on a service that tib scaffolded, by name. */
  static final Map<String, Closure<Result>> COMMANDS = [
    'tib status': { Tib tib -> tib.status() },
    'tib generate entity': { Tib tib -> tib.generate('--spec', Tib.SPEC) }
  ]

  @TempDir
  Path dir

  Tib tib

  def setup() {
    tib = new Tib(dir)
  }

  def "a directory that is not a tib project is refused by #command"() {
    when:
    Result result = COMMANDS[command](tib)

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == "${tib.root} is not a tib project: there is no .tib/state.yaml\nRun tib init service first.\n"
    dir.toFile().list().length == 0

    where:
    command << COMMANDS.keySet()
  }

  def "a state file that a newer tib wrote is refused by #command"() {
    given:
    tib.init('vehicle-service')
    Map<String, String> before = tib.files()

    when:
    Result result = COMMANDS[command](new Tib(dir, '1.2.2'))

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == "${tib.root}: .tib/state.yaml was written by tib 1.2.3, which is newer than this tib 1.2.2; upgrade tib\n"
    tib.files() == before

    where:
    command << COMMANDS.keySet()
  }

  def "a state file that tib cannot read is reported: #state"() {
    given:
    Files.createDirectories(dir.resolve('.tib'))
    tib.write(State.PATH, state)

    when:
    Result result = tib.status()

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err().startsWith("${tib.root}: $message")
    result.err().readLines().size() == 1

    where:
    state                                                                         | message
    '['                                                                           | 'the state file has a problem: state: '
    'tib: 1.2.3\npack: { name: default, version: 1 }\nspec:\n  tib: 1\n  service: int\n' | "the state file has a problem: spec.service: 'int' makes the package 'int'"
  }
}
