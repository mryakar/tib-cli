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

/** What the generators produce for an entity besides the entity itself. */
@Doc("What is generated for the entity besides the entity itself.")
public record Options(
    @Doc(value = "Whether the entity gets DTOs.", defaultValue = "true") Boolean dto,
    @Doc(value = "Whether the entity gets a mapper between it and its DTOs.", defaultValue = "true")
        Boolean mapper,
    @Doc(value = "Whether the queries of the entity are paginated.", defaultValue = "true")
        Boolean pagination,
    @Doc(value = "Whether changes of the entity are published as events.", defaultValue = "false")
        Boolean events) {

  public Options {
    dto = dto == null || dto;
    mapper = mapper == null || mapper;
    pagination = pagination == null || pagination;
    events = events != null && events;
  }

  /** The options an entity gets when the spec gives none. */
  public static Options defaults() {
    return new Options(null, null, null, null);
  }
}
