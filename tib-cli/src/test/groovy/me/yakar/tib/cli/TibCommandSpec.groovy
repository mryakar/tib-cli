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

class TibCommandSpec extends Specification {

  Tib tib = new Tib()

  def "#args prints the version of the build"() {
    when:
    Result result = tib.run(*args)

    then:
    result.exitCode() == 0
    result.out() == 'tib 1.2.3\n'

    where:
    args << [['--version'], ['init', 'service', '--version'], ['generate', 'entity', '-V']]
  }

  def "#args without a subcommand is a usage error"() {
    when:
    Result result = tib.run(*args)

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().startsWith("Missing required subcommand\nUsage: ${(['tib'] + args).join(' ')} ")

    where:
    args << [[], ['init'], ['generate']]
  }

  def "tib init --help prints the usage with the exit codes, with exit code 0"() {
    when:
    Result result = tib.run('init', '--help')

    then:
    result.exitCode() == 0
    result.out().startsWith('Usage: tib init ')
    result.out().endsWith('''\
      |Exit codes:
      |  0   Done, or nothing to change.
      |  1   The spec, the DDL, the pack or the project has a problem; the message
      |        says which.
      |  2   The command line is wrong.
      |'''.stripMargin())
    result.err() == ''
  }
}
