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
import me.yakar.tib.pack.GeneratedFiles;
import me.yakar.tib.pack.GradleWrapper;
import me.yakar.tib.pack.Sources;
import me.yakar.tib.pack.Templates;
import me.yakar.tib.pack.TestsModel;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.tree.ProjectFiles;

/**
 * Generates a new service: its build, its application, and the tests and the Dockerfile that its
 * spec chooses. Every file is written once and is the developer's from then on, but for the routes
 * of the API, which tib keeps in step with the entities of the spec: a new service has none.
 */
final class InitGenerator implements Generator {

  private final PlainPack pack;

  InitGenerator(PlainPack pack) {
    this.pack = pack;
  }

  @Override
  public void generate(Project project, ProjectFiles files) {
    GeneratedFiles generated = new GeneratedFiles(PlainPack.TEMPLATES, files);
    Map<String, Object> model =
        Map.of("project", project, "pack", pack, "tests", new TestsModel(project));
    String base = project.basePackage();
    String application = project.applicationClass();
    generated.onceWithService("settings.gradle.kts", "init/settings_gradle_kts.jte", model);
    generated.onceWithService("build.gradle.kts", "init/build_gradle_kts.jte", model);
    GradleWrapper.write(files);
    generated.onceWithService(".gitignore", "init/gitignore.jte", model);
    generated.onceWithService(".gitattributes", "init/gitattributes.jte", model);
    generated.onceWithService(Sources.main(base, application), "init/application_java.jte", model);
    generated.onceWithService(Sources.main(base, "Config"), "init/config_java.jte", model);
    generated.onceWithService(Sources.main(base, "Json"), "init/json_java.jte", model);
    generated.onceWithService(
        Sources.main(base, "Transactions"), "init/transactions_java.jte", model);
    // The application starts the routes, which follow the entities: the first run has none.
    EntityGenerator.routes(project, List.of(), generated);
    generated.onceWithService(
        Sources.test(base, "TestDatabase"), "init/test_database_java.jte", model);
    // The test of the application and the tests of the API start the service.
    if (project.tests(TestKind.INTEGRATION) || project.tests(TestKind.API)) {
      generated.onceWithService(
          Sources.test(base, "TestService"), "init/test_service_java.jte", model);
    }
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
