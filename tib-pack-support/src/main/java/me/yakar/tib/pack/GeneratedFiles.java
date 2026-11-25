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
import me.yakar.tib.generate.Project;
import me.yakar.tib.tree.ProjectFiles;

/**
 * The files that a generator of a pack stages: each a template of the pack with its parameters,
 * under the header that states its owner.
 */
public final class GeneratedFiles {

  private final Templates templates;
  private final ProjectFiles files;

  public GeneratedFiles(Templates templates, ProjectFiles files) {
    this.templates = templates;
    this.files = files;
  }

  /** Stages a file of tib's, written on every run. */
  public void own(String path, String template, Map<String, Object> parameters) {
    files.own(path, templates.render(path, Header.EVERY_RUN.text(), template, parameters));
  }

  /** Stages a file of the developer's, written once, with a new service. */
  public void onceWithService(String path, String template, Map<String, Object> parameters) {
    once(path, Header.SERVICE, template, parameters);
  }

  /** Stages a file of the developer's, written once, when its entity is created. */
  public void onceWithEntity(String path, String template, Map<String, Object> parameters) {
    once(path, Header.ENTITY, template, parameters);
  }

  /**
   * Stages a class of the base package that is the developer's, written once, with the first entity
   * that needs it. Its template takes the project.
   */
  public void onceWithFirstEntity(Project project, String type, String template) {
    once(
        Sources.main(project.basePackage(), type),
        Header.FIRST_ENTITY,
        template,
        Map.of("project", project));
  }

  private void once(String path, Header header, String template, Map<String, Object> parameters) {
    files.once(path, templates.render(path, header.text(), template, parameters));
  }
}
