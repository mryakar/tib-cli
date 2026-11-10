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
import picocli.CommandLine
import spock.lang.Specification
import spock.lang.TempDir

class GenerationOptionsSpec extends Specification {

  @TempDir
  Path dir

  @TempDir
  Path copy

  Tib tib

  def setup() {
    tib = new Tib(dir)
  }

  String spec = EXAMPLES.resolve('vehicle-service.yaml').toString()

  def "a service whose tracked files have uncommitted changes is refused, and nothing is written"() {
    given:
    tib.init('vehicle-service')
    committed()
    Files.writeString(dir.resolve('AGENTS.md'), 'mine\n')
    Map<String, String> before = tib.files()

    when:
    Result result = tib.generate('--spec', spec)

    then:
    result.exitCode() == 1
    result.out() == ''
    result.err() == """\
      ${tib.root} has changes that are not committed:
         M AGENTS.md
      Commit or stash them first, so that the changes of tib can be told apart, or run again with --allow-dirty.
      """.stripIndent()
    tib.files() == before

    when: 'a dry run writes nothing anyway'
    Result dryRun = tib.generate('--spec', spec, '--dry-run')

    then:
    dryRun.exitCode() == 0
    dryRun.out().contains('+++ b/.tib/state.yaml\n')
    tib.files() == before
  }

  def "the lines of the message that refuses uncommitted changes end the same on every platform"() {
    given:
    tib.init('vehicle-service')
    committed()
    Files.writeString(dir.resolve('AGENTS.md'), 'mine\n')
    Exception refused = null

    when: 'the command runs without the application, which prints the message'
    TibApplication.commandLine(new CommandFactory(new TibVersion('1.2.3')))
        .setExecutionExceptionHandler { Exception e, CommandLine commandLine, CommandLine.ParseResult parsed ->
          refused = e
          1
        }
        .execute('generate', 'entity', '--spec', spec, '-d', dir.toString())

    then:
    refused.message == "${tib.root} has changes that are not committed:\n" +
        '   M AGENTS.md\n' +
        'Commit or stash them first, so that the changes of tib can be told apart, or run again with --allow-dirty.'
  }

  def "the diff of a dry run of text files is a patch that git applies, with the result of a real run that #run"() {
    given: 'a committed service, and a copy of it, each with the spec in it'
    [dir, copy].each {
      Tib service = new Tib(it)
      service.init('vehicle-service')
      String all = service.write('spec.yaml', Files.readString(Path.of(spec)))
      if (file != 'spec.yaml') {
        service.generate('--spec', all)
        service.write(file, Tib.DEPOT_ONLY)
      }
    }
    committed()

    when: 'the diff of a dry run is applied to the service, and the copy is generated into'
    Result dryRun = tib.generate('--spec', dir.resolve(file).toString(), '--dry-run')
    Files.writeString(dir.resolve('.git/tib.diff'), dryRun.out())
    git('apply', '--check', '.git/tib.diff')
    git('apply', '.git/tib.diff')
    Result result = new Tib(copy).generate('--spec', copy.resolve(file).toString())

    then:
    result.out().contains('Deleted ') == (file != 'spec.yaml')
    tib.files() == new Tib(copy).files()

    where:
    run                                                 | file
    'writes the files of the entities'                  | 'spec.yaml'
    'deletes the files of an entity that left the spec' | 'depot.yaml'
  }

  def "with --allow-dirty, a service with uncommitted changes is written, with a warning"() {
    given:
    tib.init('vehicle-service')
    committed()
    (1..12).each { Files.writeString(dir.resolve("File${it}.txt"), 'x\n') }
    git('add', '.')
    git('commit', '-q', '-m', 'files')
    (1..12).each { Files.writeString(dir.resolve("File${it}.txt"), 'y\n') }

    when:
    Result result = tib.generate('--spec', spec, '--allow-dirty')

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')
    result.err().readLines() == [
      "${tib.root} has changes that are not committed:".toString(),
      *(['1', '10', '11', '12', '2', '3', '4', '5', '6', '7'].collect {
        "   M File${it}.txt".toString()
      }),
      '  and 2 more',
      'tib writes anyway, as --allow-dirty asks.'
    ]
  }

  def "untracked files, and a run that changes nothing, are no reason to refuse"() {
    given:
    tib.init('vehicle-service')
    committed()
    Files.writeString(dir.resolve('notes.txt'), 'untracked\n')

    when:
    Result result = tib.generate('--spec', spec)

    then:
    result.exitCode() == 0
    result.out().startsWith('Wrote ')

    when: 'the files of tib are not committed, and the run changes nothing'
    Result again = tib.generate('--spec', spec)

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

  /**
   * Runs git in the service, without the signing, the identity or the line endings of the machine;
   * its output is read while it runs, so that git never waits for the test.
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
    .execute(null, dir.toFile())
    StringBuilder out = new StringBuilder()
    StringBuilder err = new StringBuilder()
    process.waitForProcessOutput(out, err)
    assert process.exitValue() == 0 : err
  }
}
