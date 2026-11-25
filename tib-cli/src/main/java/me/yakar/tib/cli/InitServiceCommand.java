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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import me.yakar.tib.generate.PackException;
import me.yakar.tib.generate.Packs;
import me.yakar.tib.spec.Architecture;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecException;
import me.yakar.tib.spec.TestKind;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Tree;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.TypeConversionException;

/** {@code tib init service}. */
@Command(
    name = "service",
    description = {
      "Scaffolds a new service, from its name or its spec file.",
      "Without a name and without a spec file, tib asks for the name and for what the options"
          + " choose, on standard error, and reads the answers from standard input."
    })
class InitServiceCommand implements Callable<Integer> {

  /** What the name of a service is, for the help and for the question. */
  static final String NAME = "The name of the service, in lower case words joined with hyphens";

  @ArgGroup private Source source;

  /** Where the service comes from: its name and what it is made with, or its spec file. */
  static class Source {

    @ArgGroup(exclusive = false, multiplicity = "1")
    Named named;

    @Option(
        names = "--spec",
        required = true,
        paramLabel = "FILE",
        description = SpecFile.DESCRIPTION)
    Path spec;
  }

  /** A service by its name, with what the spec of the service would otherwise say. */
  static class Named {

    @Parameters(arity = "1", paramLabel = "NAME", description = NAME + ".")
    String name;

    @Option(
        names = "--pack",
        paramLabel = "PACK",
        converter = PackConverter.class,
        description =
            "The template pack that generates the service: default (the default), a Spring Boot"
                + " service, or plain, a service in plain Java without a dependency injection"
                + " framework.")
    String pack;

    @Option(
        names = "--java",
        paramLabel = "VERSION",
        description = "The Java version of the service: 21 (the default) or 25.")
    Integer java;

    @Option(
        names = "--architecture",
        paramLabel = "ARCHITECTURE",
        converter = ArchitectureConverter.class,
        description =
            "How the code of the service is arranged: anemic (the default), ddd or hexagonal.")
    Architecture architecture;

    @Option(
        names = "--tests",
        paramLabel = "KINDS",
        converter = TestKindsConverter.class,
        description =
            "The kinds of tests that tib writes, separated by commas: unit, integration, api and"
                + " archunit, or none; unit, integration and api by default.")
    TestKinds tests;

    @Option(
        names = "--docker",
        description = "Writes a Dockerfile that builds the service into an image.")
    Boolean docker;

    /**
     * The spec that the name and the options make, checked.
     *
     * @throws SpecException when the spec refuses one of them
     */
    Spec spec() {
      return Spec.of(name, pack, java, architecture, tests == null ? null : tests.list(), docker);
    }
  }

  /**
   * Reads a pack as a spec names it: by its name, with its version after an at sign to pin it. The
   * pack has to be on the class path.
   */
  static class PackConverter implements ITypeConverter<String> {

    @Override
    public String convert(String value) {
      try {
        Packs.find(value);
      } catch (PackException e) {
        throw new TypeConversionException(
            "'" + value + "' is not one of " + String.join(", ", Packs.names()));
      }
      return value;
    }
  }

  /** The kinds of tests that an option or an answer names. */
  record TestKinds(List<TestKind> list) {}

  /**
   * Reads kinds of tests by their names, separated by commas, as in {@code unit,api}; {@code none}
   * is no kind at all.
   */
  static class TestKindsConverter implements ITypeConverter<TestKinds> {

    @Override
    public TestKinds convert(String value) {
      if (value.equals("none")) {
        return new TestKinds(List.of());
      }
      List<TestKind> kinds = new ArrayList<>();
      for (String kind : value.split(",", -1)) {
        kinds.add(named(TestKind.values(), kind.strip()));
      }
      return new TestKinds(kinds);
    }
  }

  /** Reads an architecture by its name, as in {@code anemic}. */
  static class ArchitectureConverter implements ITypeConverter<Architecture> {

    @Override
    public Architecture convert(String value) {
      return named(Architecture.values(), value);
    }
  }

  /** The value whose name is the one given. */
  private static <E extends Enum<E>> E named(E[] values, String name) {
    for (E value : values) {
      if (value.toString().equals(name)) {
        return value;
      }
    }
    throw new TypeConversionException("'" + name + "' is not one of " + names(values));
  }

  @CommandLine.Spec private CommandSpec commandSpec;

  @Mixin private ServiceDirectory directory;

  @Mixin private GenerationRun run;

  @Mixin private Questions questions;

  @Override
  public Integer call() {
    Tree tree = directory.tree();
    if (tree.exists(State.PATH)) {
      throw new RefusalException(
          tree.root() + " is already a tib project: " + State.PATH + " is there");
    }
    if (source == null) {
      return run.init(ServiceQuestions.ask(questions), List.of(), tree);
    }
    if (source.spec != null) {
      SpecFile file = SpecFile.read(source.spec, tree);
      return run.init(file.spec(), List.of(file.input()), tree);
    }
    return run.init(named(), List.of(), tree);
  }

  /**
   * The spec that the name and the options make. A value among them that the spec refuses is a
   * mistake of the command line.
   */
  private Spec named() {
    try {
      return source.named.spec();
    } catch (SpecException e) {
      throw new ParameterException(commandSpec.commandLine(), String.join("\n", e.problems()), e);
    }
  }

  /** The names of the values, as in {@code unit, integration}. */
  static String names(Object[] values) {
    return Arrays.stream(values).map(Object::toString).collect(Collectors.joining(", "));
  }
}
