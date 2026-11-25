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
package me.yakar.tib.cli;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import me.yakar.tib.cli.InitServiceCommand.ArchitectureConverter;
import me.yakar.tib.cli.InitServiceCommand.PackConverter;
import me.yakar.tib.cli.InitServiceCommand.TestKindsConverter;
import me.yakar.tib.generate.Packs;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecException;
import me.yakar.tib.spec.TestKind;
import picocli.CommandLine.ITypeConverter;

/**
 * The questions of {@code tib init service}: the name of the service and what the options choose,
 * in the order of the command line.
 */
final class ServiceQuestions {

  private ServiceQuestions() {}

  /** The spec of the service, from the answers to the questions. */
  static Spec ask(Questions questions) {
    Spec defaults =
        questions.ask(InitServiceCommand.NAME, "", answer -> checked(() -> Spec.of(answer)));
    String name = defaults.service();
    String pack =
        questions.ask(
            "Its template pack: " + String.join(", ", Packs.names()),
            defaults.pack(),
            answer -> read(new PackConverter(), answer));
    Integer java =
        questions.ask(
            "Its Java version: 21 or 25",
            String.valueOf(defaults.java()),
            answer -> checked(() -> Spec.of(name, null, number(answer), null, null, null)).java());
    Architecture architecture =
        questions.ask(
            "Its architecture: " + InitServiceCommand.names(Architecture.values()),
            defaults.architecture().toString(),
            answer -> read(new ArchitectureConverter(), answer));
    List<TestKind> tests =
        questions.ask(
            "The kinds of its tests, separated by commas, or none: "
                + InitServiceCommand.names(TestKind.values()),
            defaults.tests().stream().map(TestKind::toString).collect(Collectors.joining(",")),
            answer -> {
              List<TestKind> kinds = read(new TestKindsConverter(), answer).list();
              return checked(() -> Spec.of(name, null, null, null, kinds, null)).tests();
            });
    Boolean docker = questions.ask("A Dockerfile: yes or no", "no", ServiceQuestions::yes);
    return Spec.of(name, pack, java, architecture, tests, docker);
  }

  /**
   * The value that the converter reads from the answer; an answer that it cannot read is refused
   * with its reason.
   */
  private static <T> T read(ITypeConverter<T> converter, String answer) {
    try {
      return converter.convert(answer);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /** The spec; one with problems is refused with them as the reason. */
  private static Spec checked(Supplier<Spec> spec) {
    try {
      return spec.get();
    } catch (SpecException e) {
      throw new IllegalArgumentException(String.join("\n", e.problems()), e);
    }
  }

  private static Integer number(String answer) {
    try {
      return Integer.valueOf(answer);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("'" + answer + "' is not a number", e);
    }
  }

  private static Boolean yes(String answer) {
    return switch (answer.toLowerCase(Locale.ROOT)) {
      case "y", "yes" -> true;
      case "n", "no" -> false;
      default -> throw new IllegalArgumentException("'" + answer + "' is not yes or no");
    };
  }
}
