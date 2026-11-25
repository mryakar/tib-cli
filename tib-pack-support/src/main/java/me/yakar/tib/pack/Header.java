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

/** What the first line of a generated file says: who owns the file, and when tib writes it. */
enum Header {

  /** A file of tib's. */
  EVERY_RUN("Written by tib on every run. Do not edit: your changes would be lost."),

  /** A file of the developer's that comes with a new service. */
  SERVICE("Written by tib when the service was created; it is yours from then on."),

  /** A file of the developer's that comes with its entity. */
  ENTITY("Written by tib when the entity was created; it is yours from then on."),

  /** A file of the developer's that the service needs once it has entities. */
  FIRST_ENTITY("Written by tib with the first entity of the service; it is yours from then on."),

  /** A migration, which is the developer's. */
  MIGRATION(
      "Written by tib when the entity was created. A change of the schema is a new migration.");

  private final String text;

  Header(String text) {
    this.text = text;
  }

  String text() {
    return text;
  }
}
