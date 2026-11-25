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
package me.yakar.tib.pack.spring

import java.nio.file.Files
import me.yakar.tib.ddl.DdlReader
import me.yakar.tib.generate.Pack
import me.yakar.tib.pack.PackAcceptanceSpec
import me.yakar.tib.pack.Services
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecReader

/**
 * The services of the default pack: beside the ones that every pack builds, the ones of the DDL of
 * the example service and of the example of what pg_dump writes.
 */
class GeneratedServiceSpec extends PackAcceptanceSpec {

  @Override
  Pack pack() {
    new SpringPack()
  }

  @Override
  Map<String, Spec> others() {
    [
      // Only the unit tests, which need no database.
      'example DDL': DdlReader.parse(Files.readString(Services.EXAMPLES.resolve('vehicle-service.sql')), SpecReader.parse('tib: 1\nservice: vehicle-service\ntests: [unit]\n')),
      // What pg_dump 18 wrote of a database: an identifier in an integer column, a text, a
      // one-to-one; with the tests of the API only.
      'example pg_dump': DdlReader.parse(Files.readString(Services.EXAMPLES.resolve('pg-dump-18.sql')), SpecReader.parse('tib: 1\nservice: vehicle-service\ntests: [api]\n')),
    ]
  }
}
