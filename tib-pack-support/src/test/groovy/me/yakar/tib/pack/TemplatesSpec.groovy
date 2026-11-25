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
package me.yakar.tib.pack

import gg.jte.TemplateNotFoundException
import me.yakar.tib.spec.Architecture
import spock.lang.Shared
import spock.lang.Specification

class TemplatesSpec extends Specification {

  /** The templates of the tests, which the build compiles into a package of their own. */
  @Shared
  Templates templates = new Templates('gg.jte.generated.support')

  def "a file is its header, then the template with the parameters, formatted if it is Java"() {
    expect:
    templates.render(path, 'Mine.', template, [name: 'A']) == text

    where:
    path          | template           | text
    'A.java'      | 'class_java.jte'   | '// Mine.\nclass A {\n  void a() {}\n}\n'
    'a.yaml'      | 'class_yaml.jte'   | '# Mine.\nname:   A\n'
  }

  def "the templates are the ones of the package: another package has none of them"() {
    when:
    new Templates('gg.jte.generated.none').render('A.java', 'Mine.', 'class_java.jte', [name: 'A'])

    then:
    TemplateNotFoundException e = thrown()
    e.message == 'Failed to load class_java.jte'
  }

  def "the templates that an architecture has to itself have its prefix: #architecture"() {
    expect:
    Templates.prefix(architecture) == prefix

    where:
    architecture           | prefix
    Architecture.ANEMIC    | ''
    Architecture.DDD       | 'ddd_'
    Architecture.HEXAGONAL | 'hexagonal_'
  }

  def "a header is a comment of the file's kind: #path"() {
    expect:
    Templates.comment(path, 'Mine.') == comment

    where:
    path                     | comment
    'src/main/java/A.java'   | '// Mine.'
    'build.gradle.kts'       | '// Mine.'
    'application.yaml'       | '# Mine.'
    '.gitignore'             | '# Mine.'
    '.gitattributes'         | '# Mine.'
    'Dockerfile'             | '# Mine.'
    '.dockerignore'          | '# Mine.'
    'db/migration/V1__a.sql' | '-- Mine.'
    'AGENTS.md'              | '<!-- Mine. -->'
  }

  def "a file of a kind without comments gets no header"() {
    when:
    Templates.comment('logo.png', 'Mine.')

    then:
    IllegalArgumentException e = thrown()
    e.message == 'there is no comment in logo.png'
  }

  def "Java is formatted as google-java-format lays it out, with its imports sorted and the unused ones left out"() {
    expect:
    Templates.format('A.java', '''\
package a;
import java.util.Set;
import java.util.List;
class A {   List<String> names() { return List.of(); } }
''') == '''\
package a;

import java.util.List;

class A {
  List<String> names() {
    return List.of();
  }
}
'''
  }

  def "a file that is not Java is left as it is"() {
    expect:
    Templates.format('application.yaml', 'a:   b\n') == 'a:   b\n'
  }

  def "Java that the formatter cannot read is a mistake of the pack, named by its path"() {
    when:
    Templates.format('A.java', 'class {')

    then:
    IllegalStateException e = thrown()
    e.message == 'A.java is not Java that google-java-format reads'
  }
}
