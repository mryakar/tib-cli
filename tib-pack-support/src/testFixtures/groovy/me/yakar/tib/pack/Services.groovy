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
package me.yakar.tib.pack

import java.nio.file.Files
import java.nio.file.Path
import me.yakar.tib.generate.Pack
import me.yakar.tib.spec.Architecture
import me.yakar.tib.spec.Spec
import me.yakar.tib.spec.SpecReader
import me.yakar.tib.spec.TestKind

/**
 * The specs of the services that the tests of every pack generate: the example service of the
 * documentation, and the corner service in every architecture.
 */
final class Services {

  /** The examples of the documentation. */
  static final Path EXAMPLES = Path.of(System.getProperty('tib.examples'))

  /** The name of the example service of the documentation. */
  static final String VEHICLES = 'vehicle-service'

  /** The architectures of the corner service, by the names of its snapshots. */
  static final Map<String, Architecture> CORNERS = [
    'corner-service': Architecture.ANEMIC,
    'corner-service-ddd': Architecture.DDD,
    'corner-service-hexagonal': Architecture.HEXAGONAL,
  ].asImmutable()

  /** The names of the services, each the name of its snapshot. */
  static final List<String> NAMES = [VEHICLES, *CORNERS.keySet()].asImmutable()

  /** The spec of the corner service, without a pack and an architecture. */
  private static final String CORNER_SPEC = Services.getResource('corner-service.yaml').text

  /**
   * What the corner spec gives and its migrations do not read back, each with what they read back:
   * a BigDecimal with a precision and no scale is NUMERIC(10, 0), which reads back as a Long, and a
   * String without a length is VARCHAR(255), which reads back with that length.
   */
  private static final Map<String, String> READ_BACK = [
    '{ name: amount, type: BigDecimal, precision: 10 }': '{ name: amount, type: Long }',
    'type: String, nullable: false }': 'type: String, length: 255, nullable: false }',
    '{ name: label, type: String }': '{ name: label, type: String, length: 255 }',
    '{ name: item, type: String }': '{ name: item, type: String, length: 255 }',
  ].asImmutable()

  private Services() {}

  /** The spec of the service of the name, of the pack. */
  static Spec named(String name, Pack pack) {
    name == VEHICLES ? vehicles(pack) : corners(pack, architecture(name))
  }

  /** The spec of the service of the name, of the pack, as its migrations read back. */
  static Spec readBack(String name, Pack pack) {
    if (name == VEHICLES) {
      return vehicles(pack)
    }
    String yaml = READ_BACK.inject(CORNER_SPEC) { String spec, String given, String read ->
      assert spec.contains(given) : "the corner spec has no '$given'"
      spec.replace(given, read)
    }
    return of(SpecReader.parse(yaml), pack, architecture(name))
  }

  /** The spec of the example service of the documentation, of the pack. */
  static Spec vehicles(Pack pack) {
    Spec spec = SpecReader.parse(Files.readString(EXAMPLES.resolve('vehicle-service.yaml')))
    return of(spec, pack, spec.architecture())
  }

  /** The spec of the corner service of the pack, in the architecture. */
  static Spec corners(Pack pack, Architecture architecture) {
    of(SpecReader.parse(CORNER_SPEC), pack, architecture)
  }

  /** The spec as one of the pack, in the architecture, with the kinds of tests. */
  static Spec of(Spec spec, Pack pack, Architecture architecture, List<TestKind> tests = spec.tests()) {
    new Spec(spec.tib(), spec.service(), spec.group(), pack.name(), spec.java(), architecture, tests, spec.docker(), spec.entities())
  }

  private static Architecture architecture(String name) {
    assert CORNERS.containsKey(name) : "no service is named $name"
    return CORNERS[name]
  }
}
