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
package me.yakar.tib.pack.spring;

import java.util.List;
import java.util.Map;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.pack.GeneratedFiles;
import me.yakar.tib.pack.Sources;
import me.yakar.tib.pack.Templates;
import me.yakar.tib.pack.TestsModel;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates a new service: the build and its Gradle wrapper, the application, its configuration, a
 * test and a main against a database in a container, and the notes for agents. Every file is
 * written once and is the developer's from then on.
 */
final class InitGenerator implements Generator {

  /**
   * The Gradle wrapper, as the build of tib has it: the files of Gradle's, without a header of
   * tib's; {@code gradlew} is written as a file that can be run.
   */
  static final List<String> WRAPPER =
      List.of(
          "gradlew",
          "gradlew.bat",
          "gradle/wrapper/gradle-wrapper.jar",
          "gradle/wrapper/gradle-wrapper.properties");

  private final SpringPack pack;

  InitGenerator(SpringPack pack) {
    this.pack = pack;
  }

  @Override
  public void generate(Project project, ProjectFiles files) {
    GeneratedFiles generated = new GeneratedFiles(SpringPack.TEMPLATES, files);
    Map<String, Object> model =
        Map.of("project", project, "pack", pack, "tests", new TestsModel(project));
    String base = project.basePackage();
    String application = project.applicationClass();
    generated.onceWithService("settings.gradle.kts", "init/settings_gradle_kts.jte", model);
    generated.onceWithService("build.gradle.kts", "init/build_gradle_kts.jte", model);
    for (String file : WRAPPER) {
      files.once(file, SpringPack.resource("wrapper/" + file), file.equals("gradlew"));
    }
    generated.onceWithService(".gitignore", "init/gitignore.jte", model);
    generated.onceWithService(".gitattributes", "init/gitattributes.jte", model);
    generated.onceWithService(Sources.main(base, application), "init/application_java.jte", model);
    generated.onceWithService(
        "src/main/resources/application.yaml", "init/application_yaml.jte", model);
    generated.onceWithService(
        Sources.test(base, "TestcontainersConfiguration"),
        "init/testcontainers_configuration_java.jte",
        model);
    // The test of the application starts it against the database, as the integration tests do.
    if (project.tests(TestKind.INTEGRATION)) {
      generated.onceWithService(
          Sources.test(base, application + "Tests"), "init/application_tests_java.jte", model);
    }
    if (project.tests(TestKind.ARCHUNIT)) {
      generated.onceWithService(
          Sources.test(base, "ArchitectureTests"),
          "init/" + Templates.prefix(project.architecture()) + "architecture_tests_java.jte",
          model);
    }
    generated.onceWithService(
        Sources.test(base, "Test" + application), "init/test_application_java.jte", model);
    if (project.docker()) {
      generated.onceWithService("Dockerfile", "init/dockerfile.jte", model);
      generated.onceWithService(".dockerignore", "init/dockerignore.jte", model);
    }
    generated.onceWithService("AGENTS.md", "init/agents_md.jte", model);
  }
}
