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
package me.yakar.tib.pack;

import java.util.Map;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.tree.Tree;

/**
 * Generates a new service: the build, the application, its configuration, a test and a main against
 * a database in a container, and the notes for agents. Every file is written once and is the
 * developer's from then on.
 */
final class InitGenerator implements Generator {

  private static final String HEADER =
      "Written by tib when the service was created; it is yours from then on.";

  private final DefaultPack pack;

  InitGenerator(DefaultPack pack) {
    this.pack = pack;
  }

  @Override
  public void generate(Project project, Tree tree) {
    String sources = DefaultPack.MAIN + project.packagePath();
    String tests = DefaultPack.TEST + project.packagePath();
    once(tree, "settings.gradle.kts", "init/settings_gradle_kts.jte", project);
    once(tree, "build.gradle.kts", "init/build_gradle_kts.jte", project);
    once(tree, ".gitignore", "init/gitignore.jte", project);
    once(
        tree,
        sources + "/" + project.applicationClass() + ".java",
        "init/application_java.jte",
        project);
    once(tree, "src/main/resources/application.yaml", "init/application_yaml.jte", project);
    once(
        tree,
        tests + "/TestcontainersConfiguration.java",
        "init/testcontainers_configuration_java.jte",
        project);
    once(
        tree,
        tests + "/" + project.applicationClass() + "Tests.java",
        "init/application_tests_java.jte",
        project);
    once(
        tree,
        tests + "/Test" + project.applicationClass() + ".java",
        "init/test_application_java.jte",
        project);
    once(tree, "AGENTS.md", "init/agents_md.jte", project);
  }

  private void once(Tree tree, String path, String template, Project project) {
    tree.once(
        path, Templates.render(path, HEADER, template, Map.of("project", project, "pack", pack)));
  }
}
