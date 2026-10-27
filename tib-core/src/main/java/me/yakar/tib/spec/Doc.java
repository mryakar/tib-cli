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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What a property of the spec means and what it accepts. The JSON Schema of the spec is generated
 * from these annotations, and the spec is checked against the same rules, so the two cannot drift
 * apart.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE})
public @interface Doc {

  /** The description, for the schema. */
  String value();

  /** Whether the spec must give the property. */
  boolean required() default false;

  /** The regular expression a string property must match, if any. */
  String pattern() default "";

  /** The lowest value an integer property may have, if any. */
  long minimum() default NO_MINIMUM;

  /** The value the property gets when the spec gives none, if it is a literal. */
  String defaultValue() default "";

  long NO_MINIMUM = Long.MIN_VALUE;
}
