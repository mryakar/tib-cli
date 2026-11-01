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
package me.yakar.tib

import java.nio.charset.MalformedInputException
import java.nio.file.AccessDeniedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import spock.lang.Specification

class TibExceptionSpec extends Specification {

  def "the reason of #exception.class.simpleName is '#reason'"() {
    expect:
    TibException.reason(exception) == reason

    where:
    exception                                        | reason
    new NoSuchFileException('f')                     | 'no such file'
    new AccessDeniedException('f')                   | 'access denied'
    new FileAlreadyExistsException('f')              | 'already exists'
    new NotDirectoryException('f')                   | 'not a directory'
    new FileSystemException('f', null, 'Is a directory') | 'f: Is a directory'
    new MalformedInputException(1)                   | 'Input length = 1'
  }
}
