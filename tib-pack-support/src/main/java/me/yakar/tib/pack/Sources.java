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

/**
 * The Java sources of a generated service: where the file of a class is, and how a file imports a
 * class.
 */
public final class Sources {

  private Sources() {}

  /** The file of a class of the service, relative to the project. */
  public static String main(String packageName, String type) {
    return file("src/main/java/", packageName, type);
  }

  /** The file of a class of the tests of the service, relative to the project. */
  public static String test(String packageName, String type) {
    return file("src/test/java/", packageName, type);
  }

  private static String file(String root, String packageName, String type) {
    return root + packageName.replace('.', '/') + "/" + type + ".java";
  }

  /**
   * The import of a type into a file of the package, as a line of Java; none when the type is in
   * the package already.
   */
  public static String importOf(String filePackage, String typePackage, String type) {
    return filePackage.equals(typePackage) ? "" : "import " + typePackage + "." + type + ";";
  }
}
