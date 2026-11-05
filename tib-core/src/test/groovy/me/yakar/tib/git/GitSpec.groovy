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
package me.yakar.tib.git

import java.nio.file.Files
import java.nio.file.Path
import spock.lang.Specification
import spock.lang.TempDir

class GitSpec extends Specification {

  @TempDir
  Path repository

  def "a directory that is not in a repository has no uncommitted changes"() {
    expect:
    Git.uncommitted(repository) == []
    Git.uncommitted(repository.resolve('not-there')) == []
  }

  def "the uncommitted changes are those of the tracked files under the directory, staged or not"() {
    given: 'a repository with a service in it, and a file next to the service'
    Path service = Files.createDirectories(repository.resolve('service'))
    Files.writeString(service.resolve('Edited.java'), 'class Edited {}\n')
    Files.writeString(service.resolve('Staged.java'), 'class Staged {}\n')
    Files.writeString(service.resolve('Kept.java'), 'class Kept {}\n')
    Files.writeString(repository.resolve('Other.java'), 'class Other {}\n')
    commit()

    expect: 'a clean service has none'
    Git.uncommitted(service) == []

    when:
    Files.writeString(service.resolve('Edited.java'), 'class Edited { int x; }\n')
    Files.writeString(service.resolve('Staged.java'), 'class Staged { int x; }\n')
    git('add', 'service/Staged.java')
    Files.writeString(service.resolve('New.java'), 'class New {}\n')
    Files.writeString(repository.resolve('Other.java'), 'class Other { int x; }\n')

    then: 'an untracked file and a change outside the service are none'
    Git.uncommitted(service) == [' M service/Edited.java', 'M  service/Staged.java']
  }

  def "a path with letters outside ASCII is as it is on disk"() {
    given:
    Files.writeString(repository.resolve('Şube.java'), 'class Şube {}\n')
    commit()
    Files.writeString(repository.resolve('Şube.java'), 'class Şube { int edited; }\n')

    expect:
    Git.uncommitted(repository) == [' M Şube.java']
  }

  def "without git to ask, there are none"() {
    expect:
    Git.uncommitted(repository, 'no-such-git') == []
  }

  def "an interrupted thread gets the answer, and stays interrupted"() {
    given:
    Files.writeString(repository.resolve('A.java'), 'class A {}\n')
    commit()
    Files.writeString(repository.resolve('A.java'), 'class A { int edited; }\n')

    when:
    Thread.currentThread().interrupt()
    List<String> changes = Git.uncommitted(repository)

    then:
    changes == [' M A.java']
    Thread.interrupted()
  }

  /** Makes the directory a repository and commits every file in it. */
  private void commit() {
    git('init', '-q')
    git('add', '.')
    git('commit', '-q', '-m', 'first')
  }

  /**
   * Runs git in the repository, without the signing, the identity or the line endings of the
   * machine.
   */
  private void git(String... args) {
    Process process = ([
      'git',
      '-c',
      'user.name=tib',
      '-c',
      'user.email=tib@example.com',
      '-c',
      'commit.gpgsign=false',
      '-c',
      'core.autocrlf=false'
    ] + args.toList())
    .execute(null, repository.toFile())
    StringBuilder out = new StringBuilder()
    StringBuilder err = new StringBuilder()
    process.waitForProcessOutput(out, err)
    assert process.exitValue() == 0 : err
  }
}
