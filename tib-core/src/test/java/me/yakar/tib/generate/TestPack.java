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
package me.yakar.tib.generate;

import java.util.Set;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.tree.ProjectFiles;

/** A pack that the tests of the engine find on the class path, or build with a name of theirs. */
public final class TestPack implements Pack {

  private final String name;
  private final String version;
  private final Set<Architecture> architectures;

  public TestPack() {
    this("test", "0");
  }

  /** A pack that arranges a service in every architecture, as a pack does by default. */
  public TestPack(String name, String version) {
    this(name, version, null);
  }

  /** A pack that arranges a service only in the architectures, or in every one for null. */
  public TestPack(String name, String version, Set<Architecture> architectures) {
    this.name = name;
    this.version = version;
    this.architectures = architectures;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public String stack() {
    return "Test " + version;
  }

  @Override
  public Set<Architecture> architectures() {
    return architectures == null ? Pack.super.architectures() : architectures;
  }

  @Override
  public Generator init() {
    return (Project project, ProjectFiles files) -> files.own("test.txt", project.name() + "\n");
  }

  @Override
  public Generator entity() {
    return init();
  }
}
