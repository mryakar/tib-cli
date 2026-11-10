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
package me.yakar.tib.cli;

import java.util.ResourceBundle;
import picocli.CommandLine.IVersionProvider;

/** The version that {@code tib --version} prints, and that tib writes into the state file. */
final class TibVersion implements IVersionProvider {

  private final String version;

  TibVersion(String version) {
    this.version = version;
  }

  /** The version that the build wrote into the application. */
  static TibVersion ofBuild() {
    return new TibVersion(
        ResourceBundle.getBundle("me.yakar.tib.cli.version").getString("version"));
  }

  /** The version, as in {@code 0.1.0}. */
  String version() {
    return version;
  }

  @Override
  public String[] getVersion() {
    return new String[] {"tib " + version};
  }
}
