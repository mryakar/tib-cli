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

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.util.Map;
import me.yakar.tib.generate.Generator;
import me.yakar.tib.generate.Project;
import me.yakar.tib.tree.Tree;

/**
 * Generates a new service: the build, the application, its configuration, a test against a database
 * in a container, and the notes for agents. Everything but the notes is written once and is the
 * developer's from then on.
 */
final class InitGenerator implements Generator {

  private static final TemplateEngine TEMPLATES =
      TemplateEngine.createPrecompiled(ContentType.Plain);

  private final DefaultPack pack;

  InitGenerator(DefaultPack pack) {
    this.pack = pack;
  }

  @Override
  public void generate(Project project, Tree tree) {
    String sources = "src/main/java/" + project.packagePath();
    String tests = "src/test/java/" + project.packagePath();
    tree.once("settings.gradle.kts", render("init/settings_gradle_kts.jte", project));
    tree.once("build.gradle.kts", render("init/build_gradle_kts.jte", project));
    tree.once(".gitignore", render("init/gitignore.jte", project));
    tree.once(
        sources + "/" + project.applicationClass() + ".java",
        render("init/application_java.jte", project));
    tree.once("src/main/resources/application.yaml", render("init/application_yaml.jte", project));
    tree.once(
        tests + "/TestcontainersConfiguration.java",
        render("init/testcontainers_configuration_java.jte", project));
    tree.once(
        tests + "/" + project.applicationClass() + "Tests.java",
        render("init/application_tests_java.jte", project));
    tree.own("AGENTS.md", render("init/agents_md.jte", project));
  }

  private String render(String template, Project project) {
    StringOutput output = new StringOutput();
    TEMPLATES.render(template, Map.of("project", project, "pack", pack), output);
    return output.toString();
  }
}
