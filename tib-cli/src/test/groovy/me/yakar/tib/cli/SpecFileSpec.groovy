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
import me.yakar.tib.state.State
import me.yakar.tib.tree.Tree
import spock.lang.Specification
import spock.lang.TempDir

class SpecFileSpec extends Specification {

  @TempDir
  Path dir

  def "the spec is read, and the file is recorded with its path from the service and its content"() {
    given:
    Path service = Files.createDirectories(dir.resolve('vehicle-service'))
    Path file = Files.writeString(dir.resolve('spec.yaml'), 'tib: 1\nservice: vehicle-service\n')

    when:
    SpecFile read = SpecFile.read(file, Tree.at(service))

    then:
    read.spec().service() == 'vehicle-service'
    read.input() == State.Input.of(State.Input.Kind.SPEC, service, file, 'tib: 1\nservice: vehicle-service\n')
    read.input().path() == '../spec.yaml'
  }
}
