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
package me.yakar.tib.generate

import me.yakar.tib.spec.Spec
import spock.lang.Specification

class ProjectSpec extends Specification {

  def "the names of the project come from the spec: #service"() {
    given:
    Project project = Project.of(new Spec(1, service, group, null, null))

    expect:
    project.name() == service
    project.group() == (group ?: 'com.example')
    project.basePackage() == basePackage
    project.packagePath() == basePackage.replace('.', '/')
    project.applicationClass() == applicationClass
    project.spec().service() == service

    where:
    service           | group      | basePackage                      | applicationClass
    'vehicle-service' | null       | 'com.example.vehicleservice'     | 'VehicleServiceApplication'
    'ops'             | 'me.yakar' | 'me.yakar.ops'                   | 'OpsApplication'
    'a-b-c'           | 'x'        | 'x.abc'                          | 'ABCApplication'
  }
}
