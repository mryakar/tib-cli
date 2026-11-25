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

import java.util.concurrent.Callable
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IFactory
import picocli.CommandLine.ParameterException
import spock.lang.Specification

class QuestionsSpec extends Specification {

  /** A command that asks for a number. */
  @Command(name = 'ask')
  static class Ask implements Callable<Integer> {

    @CommandLine.Mixin
    Questions questions

    String defaultAnswer = '21'

    @Override
    Integer call() {
      questions.ask('A number', defaultAnswer) { String answer ->
        if (!answer.isInteger()) {
          throw new IllegalArgumentException("'$answer' is not a number")
        }
        answer as Integer
      }
    }
  }

  Ask ask = new Ask()

  StringWriter err = new StringWriter()

  def "an empty answer is the default, shown after the question, and another answer is read without the spaces around it: '#answers'"() {
    given:
    reads(new StringReader(answers))

    expect:
    ask.call() == number
    asked() == 'A number [21]: '

    where:
    answers   | number
    '\n'      | 21
    ' 42 \n'  | 42
    '42'      | 42
  }

  def "a question without a default shows none, and an empty answer is read as it is"() {
    given:
    ask.defaultAnswer = ''
    reads(new StringReader('\n42\n'))

    expect:
    ask.call() == 42
    asked() == "A number: '' is not a number\nA number: "
  }

  def "for an answer that cannot be read, the reason is printed and the question asked again"() {
    given:
    reads(new StringReader('x\n4 2\n42\n'))

    expect:
    ask.call() == 42
    asked() == "A number [21]: 'x' is not a number\nA number [21]: '4 2' is not a number\nA number [21]: "
  }

  def "answers that end before the question has one are a mistake of the command line"() {
    given:
    reads(new StringReader('x\n'))

    when:
    ask.call()

    then:
    ParameterException e = thrown()
    e.message == 'The command line leaves out what tib asks for, and there are no more answers.'
    asked() == "A number [21]: 'x' is not a number\nA number [21]: \n"
  }

  def "answers that cannot be read are refused"() {
    given:
    reads(new Reader() {
          @Override
          int read(char[] buffer, int offset, int length) {
            throw new IOException('the terminal is gone')
          }

          @Override
          void close() {}
        })

    when:
    ask.call()

    then:
    RefusalException e = thrown()
    e.message == 'cannot read the answers: the terminal is gone'
    asked() == 'A number [21]: '
  }

  /** Gives the command the answers, and its questions the standard error of the test. */
  private void reads(Reader answers) {
    IFactory factory = { Class cls ->
      cls == Questions ? new Questions(answers) : CommandLine.defaultFactory().create(cls)
    } as IFactory
    new CommandLine(ask, factory).setErr(new PrintWriter(err))
  }

  /** What the command printed on standard error, with the line separator of every platform. */
  private String asked() {
    err.toString().replace('\r\n', '\n')
  }
}
