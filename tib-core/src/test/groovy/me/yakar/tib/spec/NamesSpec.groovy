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
package me.yakar.tib.spec

import spock.lang.Specification

class NamesSpec extends Specification {

  def "the SQL name of #java is #sql"() {
    expect:
    Names.sql(java) == sql

    where:
    java          | sql
    'id'          | 'ID'
    'plateNumber' | 'PLATE_NUMBER'
    'Vehicle'     | 'VEHICLE'
    'depotID'     | 'DEPOT_ID'
    'line2Code'   | 'LINE2_CODE'
  }

  def "the plural of #name is #plural"() {
    expect:
    Names.plural(name) == plural

    where:
    name      | plural
    'vehicle' | 'vehicles'
    'branch'  | 'branches'
    'bus'     | 'buses'
    'box'     | 'boxes'
    'quiz'    | 'quizes'
    'dish'    | 'dishes'
    'entry'   | 'entries'
    'day'     | 'days'
  }

  def "the member name of #type is #member"() {
    expect:
    Names.member(type) == member

    where:
    type    | member
    'Depot' | 'depot'
    'D'     | 'd'
  }
}
