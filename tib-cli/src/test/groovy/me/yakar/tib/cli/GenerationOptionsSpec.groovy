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

import static me.yakar.tib.cli.Tib.EXAMPLES

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.cli.Tib.Result
import spock.lang.Specification
import spock.lang.TempDir

class GenerationOptionsSpec extends Specification {

  @TempDir
  Path dir

  Tib tib = new Tib()

  String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()


  def "a service whose tracked files have uncommitted changes is refused, and nothing is written"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    committed()
    Files.writeString(dir.resolve('AGENTS.md'), 'mine\n')
    Map<String, String> before = Tib.files(dir)

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == """\
      ${dir.toAbsolutePath().normalize()} has changes that are not committed:
         M AGENTS.md
      Commit or stash them first, so that the changes of tib can be told apart, or run again with --allow-dirty.
      """.stripIndent()
    Tib.files(dir) == before

    when: 'a dry run writes nothing anyway'
    Result dryRun = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--dry-run')

    then:
    dryRun.exitCode() == 0
    dryRun.out().contains('+++ b/.tib/state.yaml\n')
  }

  def "with --allow-dirty, a service with uncommitted changes is written, with a warning"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    committed()
    (1..12).each { Files.writeString(dir.resolve("File${it}.txt"), 'x\n') }
    git('add', '.')
    git('commit', '-q', '-m', 'files')
    (1..12).each { Files.writeString(dir.resolve("File${it}.txt"), 'y\n') }

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString(), '--allow-dirty')

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')
    result.err().readLines() == [
      "${dir.toAbsolutePath().normalize()} has changes that are not committed:".toString(),
      *(['1', '10', '11', '12', '2', '3', '4', '5', '6', '7'].collect {
        "   M File${it}.txt".toString()
      }),
      '  and 2 more',
      'tib writes anyway, as --allow-dirty asks.'
    ]
  }

  def "untracked files, and a run that changes nothing, are no reason to refuse"() {
    given:
    tib.run('init', 'service', 'vehicle-service', '-d', dir.toString())
    committed()
    Files.writeString(dir.resolve('notes.txt'), 'untracked\n')

    when:
    Result result = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')

    when: 'the files of tib are not committed, and the run changes nothing'
    Result again = tib.run('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    again.exitCode() == 0
    again.out() == 'Nothing to change.\n'
  }

  /** Makes the service a git repository with everything in it committed. */
  private void committed() {
    git('init', '-q')
    git('add', '.')
    git('commit', '-q', '-m', 'service')
  }

  /** Runs git in the service, without the signing or the identity of the machine. */
  private void git(String... args) {
    Process process = (['git', '-c', 'user.name=tib', '-c', 'user.email=tib@example.com', '-c', 'commit.gpgsign=false'] + args.toList())
    .execute(null, dir.toFile())
    assert process.waitFor() == 0 : process.errorStream.text
  }
}
