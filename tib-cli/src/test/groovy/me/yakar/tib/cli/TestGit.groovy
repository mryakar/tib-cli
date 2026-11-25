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

import java.nio.file.Path

/** Runs git for the tests, in a directory of theirs. */
class TestGit {

  /** Makes the directory a repository with every file in it committed. */
  static void commitAll(Path directory) {
    run(directory, 'init', '-q')
    run(directory, 'add', '.')
    run(directory, 'commit', '-q', '-m', 'first')
  }

  /**
   * Runs git in the directory, without the signing, the identity or the line endings of the
   * machine; its output is read while it runs, so that git never waits for the test.
   */
  static void run(Path directory, String... args) {
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
    .execute(null, directory.toFile())
    StringBuilder out = new StringBuilder()
    StringBuilder err = new StringBuilder()
    process.waitForProcessOutput(out, err)
    assert process.exitValue() == 0 : err
  }
}
