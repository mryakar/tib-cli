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

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import spock.lang.Specification

class TibApplicationSpec extends Specification {

  def "runs the command and keeps its exit code"() {
    given:
    TibApplication application = new TibApplication(new TibCommandSpec.TestFactory(), new TibCommand())

    when:
    application.run('--no-such-option')

    then:
    application.exitCode == 2
  }

  def "the application prints its version and exits with 0"() {
    when:
    Process process = start('--version')
    String output = process.inputStream.text.trim()

    then:
    process.waitFor(2, TimeUnit.MINUTES)
    process.exitValue() == 0
    output == 'tib ' + System.getProperty('tib.version')
  }

  def "the application exits with the exit code of the command"() {
    when:
    Process process = start('--no-such-option')
    String output = process.inputStream.text

    then:
    process.waitFor(2, TimeUnit.MINUTES)
    process.exitValue() == 2
    // Contains, not starts with: on some consoles picocli colors the message.
    output.contains("Unknown option: '--no-such-option'")
  }

  /**
   * Starts the application in a JVM of its own, as a user would, on the class path of this test.
   * The JaCoCo agent of this JVM, if any, goes with it, so that the coverage counts main().
   */
  private static Process start(String... args) {
    List<String> command = [ProcessHandle.current().info().command().orElseThrow()]
    command.addAll(ManagementFactory.runtimeMXBean.inputArguments.findAll {
      it.startsWith('-javaagent:')
    })
    command.addAll(['-cp', System.getProperty('java.class.path'), TibApplication.name])
    command.addAll(args)
    new ProcessBuilder(command).redirectErrorStream(true).start()
  }
}
