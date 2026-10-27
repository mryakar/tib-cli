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

import me.yakar.tib.spec.SpecSchema
import picocli.CommandLine
import spock.lang.Specification

class SchemaCommandSpec extends Specification {

  StringWriter out = new StringWriter()
  CommandLine tib = new CommandLine(new TibCommand(), new TibCommandSpec.TestFactory())
  .setOut(new PrintWriter(out))
  .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.OFF))

  def "tib schema prints the JSON Schema of the spec"() {
    when:
    int exitCode = tib.execute('schema')

    then:
    exitCode == 0
    out.toString() == SpecSchema.json()
    out.toString().startsWith('{\n  "$schema": "https://json-schema.org/draft/2020-12/schema"')
  }
}
