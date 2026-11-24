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
package me.yakar.tib.pack.plain;

import java.util.List;
import java.util.Map;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.pack.EntityModel;
import me.yakar.tib.pack.TestsModel;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates a new service: the build and its Gradle wrapper, the application that wires the service
 * by hand, its configuration, the support of its transactions and of its JSON, the tests that start
 * PostgreSQL in a container, a main against it, and the notes for agents. Every file is written
 * once and is the developer's from then on, but for the routes of the API, which tib keeps in step
 * with the entities of the spec: a new service has none.
 */
final class InitGenerator implements Generator {

  private static final String HEADER =
      "Written by tib when the service was created; it is yours from then on.";

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

  private final PlainPack pack;

  InitGenerator(PlainPack pack) {
    this.pack = pack;
  }

  @Override
  public void generate(Project project, ProjectFiles files) {
    String sources = EntityModel.directory(project.basePackage());
    String tests = EntityModel.testDirectory(project.basePackage());
    once(files, "settings.gradle.kts", "init/settings_gradle_kts.jte", project);
    once(files, "build.gradle.kts", "init/build_gradle_kts.jte", project);
    for (String file : WRAPPER) {
      files.once(file, PlainPack.resource("wrapper/" + file), file.equals("gradlew"));
    }
    once(files, ".gitignore", "init/gitignore.jte", project);
    once(files, ".gitattributes", "init/gitattributes.jte", project);
    once(
        files,
        sources + "/" + project.applicationClass() + ".java",
        "init/application_java.jte",
        project);
    once(files, sources + "/Config.java", "init/config_java.jte", project);
    once(files, sources + "/Json.java", "init/json_java.jte", project);
    once(files, sources + "/Transactions.java", "init/transactions_java.jte", project);
    // The application starts the routes, which follow the entities: the first run has none.
    EntityGenerator.routes(project, List.of(), files);
    once(files, tests + "/TestDatabase.java", "init/test_database_java.jte", project);
    // The test of the application and the tests of the API start the service.
    if (project.tests(TestKind.INTEGRATION) || project.tests(TestKind.API)) {
      once(files, tests + "/TestService.java", "init/test_service_java.jte", project);
    }
    if (project.tests(TestKind.INTEGRATION)) {
      once(
          files,
          tests + "/" + project.applicationClass() + "Tests.java",
          "init/application_tests_java.jte",
          project);
    }
    if (project.tests(TestKind.ARCHUNIT)) {
      once(files, tests + "/ArchitectureTests.java", "init/architecture_tests_java.jte", project);
    }
    once(
        files,
        tests + "/Test" + project.applicationClass() + ".java",
        "init/test_application_java.jte",
        project);
    if (project.docker()) {
      once(files, "Dockerfile", "init/dockerfile.jte", project);
      once(files, ".dockerignore", "init/dockerignore.jte", project);
    }
    once(files, "AGENTS.md", "init/agents_md.jte", project);
  }

  private void once(ProjectFiles files, String path, String template, Project project) {
    files.once(
        path,
        PlainPack.TEMPLATES.render(
            path,
            HEADER,
            template,
            Map.of("project", project, "pack", pack, "tests", new TestsModel(project))));
  }
}
