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
package me.yakar.tib.pack.plain

import java.nio.file.Files
import me.yakar.tib.ddl.DdlReader
import me.yakar.tib.generate.Pack
import me.yakar.tib.pack.PackAcceptanceSpec
import me.yakar.tib.pack.Services
import me.yakar.tib.spec.Architecture
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.spec.TestKind

/**
 * The services of the plain pack: beside the ones that every pack builds, the example service with
 * the tests that need no database, and the one of its DDL. Both are on Java 21 and without a
 * Dockerfile, with some of the kinds of tests: one in the hexagonal architecture, the other in the
 * DDD.
 */
class GeneratedServiceSpec extends PackAcceptanceSpec {

  @Override
  Pack pack() {
    new PlainPack()
  }

  @Override
  Map<String, Spec> others() {
    [
      // Only the unit tests and the test of the architecture, which need no database.
      'example spec, hexagonal, without a database': Services.of(Services.vehicles(pack()), pack(), Architecture.HEXAGONAL, [TestKind.UNIT, TestKind.ARCHUNIT]),
      // The tables of the DDL, with the tests of the API only.
      'example DDL, DDD': DdlReader.parse(Files.readString(Services.EXAMPLES.resolve('vehicle-service.sql')), SpecReader.parse('tib: 1\nservice: vehicle-service\npack: plain\narchitecture: ddd\ntests: [api]\n')),
    ]
  }

  /** The routes of the API, which tib writes on every run. */
  @Override
  protected List<String> classes() {
    ['Routes']
  }
}
