/*
 * Copyright $YEAR the original author or authors.
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

import me.yakar.tib.tree.Tree;

/** A pack that the tests of the engine find on the class path. */
public final class TestPack implements Pack {

  @Override
  public String name() {
    return "test";
  }

  @Override
  public String version() {
    return "0";
  }

  @Override
  public Generator init() {
    return (Project project, Tree tree) -> tree.own("test.txt", project.name() + "\n");
  }
}
