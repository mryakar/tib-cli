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
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import me.yakar.tib.cli.Tib.Result
import picocli.CommandLine.IFactory
import spock.lang.Specification
import spock.lang.TempDir

class TibApplicationSpec extends Specification {

  @TempDir
  Path dir

  Process process

  def cleanup() {
    process?.destroyForcibly()
  }

  def "a bug is reported with its stack trace, with exit code 3: #failure.class.simpleName"() {
    given: 'a command that fails with a throwable that is not tib\'s'
    Throwable thrown = failure
    CommandFactory commands = new CommandFactory(new TibVersion('1.2.3'))
    IFactory factory = { Class cls ->
      cls == SchemaCommand ? new SchemaCommand() {
            @Override
            void run() {
              throw thrown
            }
          } : commands.create(cls)
    } as IFactory
    StringWriter out = new StringWriter()
    StringWriter err = new StringWriter()

    when:
    int exitCode = TibApplication.run(TibApplication.commandLine(factory)
        .setOut(new PrintWriter(out))
        .setErr(new PrintWriter(err)), 'schema')

    then:
    exitCode == 3
    out.toString() == ''
    err.toString().startsWith("${failure.class.name}: a bug")
    err.toString().contains('\tat ')

    where: 'picocli hands an exception to the handler, and lets an error through'
    failure << [new IllegalStateException('a bug'), new StackOverflowError('a bug')]
  }

  def "the application prints its version and exits with 0"() {
    when:
    Result result = start('--version')

    then:
    result.exitCode() == 0
    result.out() == "tib ${System.getProperty('tib.version')}\n"
  }

  def "the application exits with the exit code of the command"() {
    when:
    Result result = start('--no-such-option')

    then:
    result.exitCode() == 2
    result.out() == ''
    result.err().contains('Usage: tib ')
  }

  /**
   * Runs the application in a JVM of its own, as a user would, on the class path of this test.
   * The JaCoCo agent of this JVM, if any, goes with it, so that the coverage counts main(). The
   * output goes to files, so that a child that hangs cannot block the test before the timeout.
   */
  private Result start(String... args) {
    List<String> command = [ProcessHandle.current().info().command().orElseThrow()]
    command.addAll(ManagementFactory.runtimeMXBean.inputArguments.findAll {
      it.startsWith('-javaagent:')
    })
    command.addAll(['-cp', System.getProperty('java.class.path'), TibApplication.name])
    command.addAll(args)
    File out = dir.resolve('out.txt').toFile()
    File err = dir.resolve('err.txt').toFile()
    process = new ProcessBuilder(command).redirectOutput(out).redirectError(err).start()
    assert process.waitFor(2, TimeUnit.MINUTES): 'the application did not end within 2 minutes'
    new Result(process.exitValue(), out.text.replace('\r\n', '\n'), err.text.replace('\r\n', '\n'))
  }
}
