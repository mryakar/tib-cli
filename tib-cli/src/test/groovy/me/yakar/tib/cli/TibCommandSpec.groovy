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

import me.yakar.tib.cli.docs.CommandFactory
import picocli.CommandLine
import spock.lang.Specification

class TibCommandSpec extends Specification {

  StringWriter out = new StringWriter()
  // Without colors: whether picocli would use them depends on the platform and the console.
  CommandLine tib = TibApplication.commandLine(new CommandFactory('1.2.3'))
  .setOut(new PrintWriter(out))
  .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.OFF))

  def "#args prints the version of the build"() {
    when:
    int exitCode = tib.execute(*args)

    then:
    exitCode == 0
    out.toString().trim() == 'tib 1.2.3'

    where:
    args << [['--version'], ['init', 'service', '--version'], ['generate', 'entity', '-V']]
  }

  def "without a subcommand it prints its usage"() {
    when:
    int exitCode = tib.execute()

    then:
    exitCode == 0
    out.toString().startsWith('Usage: tib')
  }
}
