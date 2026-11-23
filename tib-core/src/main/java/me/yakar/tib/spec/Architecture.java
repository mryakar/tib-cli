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
package me.yakar.tib.spec;

/** How the code of a service is arranged in packages, and which part may use which. */
public enum Architecture {
  /**
   * Every class of an entity in the package of the entity: its data, repository, service, mapper,
   * DTO and controller, the controller over the service over the repository.
   */
  ANEMIC("anemic"),

  /**
   * Every entity an aggregate in three packages: its domain, with the entity and its repository;
   * its application, with the service of its use cases; and its web, with its controller, DTO and
   * mapper. An entity refers to another by its identifier.
   */
  DDD("ddd"),

  /**
   * Every entity a domain without a framework, in the middle of ports and adapters: its domain, in
   * plain Java; its application, with the service of its use cases and the ports, the use cases
   * that the inbound adapters call and the store that the outbound ones implement; and its
   * adapters, the web with its controller, DTO and mapper, and the persistence with its JPA entity,
   * repository, mapper and the adapter of the store. An entity refers to another by its identifier.
   */
  HEXAGONAL("hexagonal");

  private final String name;

  Architecture(String name) {
    this.name = name;
  }

  /** The name in the spec. */
  @Override
  public String toString() {
    return name;
  }
}
