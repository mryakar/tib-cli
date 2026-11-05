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
package me.yakar.tib.state;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import me.yakar.tib.spec.Spec;
import me.yakar.tib.spec.SpecException;
import me.yakar.tib.tree.Owner;
import me.yakar.tib.tree.Tree;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * What tib keeps in the project about what it generated: the version of tib and the pack it ran
 * with, the files that the spec was read from, the resolved spec, and every file it wrote with its
 * owner and the hash of its content. The file is {@value #PATH}; tib writes it, nobody edits it.
 *
 * @param tib the version of tib that wrote the state
 */
public record State(String tib, PackRef pack, List<Input> inputs, Spec spec, List<File> files) {

  /** The path of the state file in a project. */
  public static final String PATH = ".tib/state.yaml";

  private static final YAMLMapper YAML =
      YAMLMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
          .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
          .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
          .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
          .build();

  /** The pack that generated the files. */
  public record PackRef(String name, String version) {}

  /**
   * A file that the spec of the service was read from, with the SHA-256 of its content when it was
   * read.
   *
   * @param path the path of the file, relative to the service when it can be, with forward slashes
   */
  public record Input(Kind kind, String path, String sha256) {

    /** What the file is. */
    public enum Kind {
      /** A spec file. */
      SPEC,
      /** SQL DDL. */
      DDL;

      /** The name in the state file. */
      @Override
      public String toString() {
        return name().toLowerCase(Locale.ROOT);
      }
    }

    /**
     * The file as it is now, with its path relative to the directory of the service.
     *
     * @throws StateException when the file cannot be read
     */
    public static Input of(Kind kind, Path service, Path file) {
      Path absolute = file.toAbsolutePath().normalize();
      byte[] content;
      try {
        content = Files.readAllBytes(absolute);
      } catch (IOException e) {
        throw new StateException("read", file, e);
      }
      return new Input(
          kind, path(service.toAbsolutePath().normalize(), absolute), State.sha256(content));
    }

    /**
     * The path of the file relative to the service, with forward slashes; a file on another root
     * than the service, as on another drive, keeps its whole path.
     */
    static String path(Path service, Path file) {
      Path path =
          Objects.equals(service.getRoot(), file.getRoot()) ? service.relativize(file) : file;
      return Objects.toString(path.getRoot(), "").replace('\\', '/')
          + StreamSupport.stream(path.spliterator(), false)
              .map(Path::toString)
              .collect(Collectors.joining("/"));
    }
  }

  /** A file that tib wrote, with its owner and the SHA-256 of its content when it was written. */
  public record File(String path, Owner owner, String sha256) {

    /** The file as tib writes it, with the hash of its content. */
    public static File of(String path, Owner owner, String content) {
      return new File(path, owner, State.sha256(content));
    }
  }

  public State {
    inputs = inputs == null ? List.of() : List.copyOf(inputs);
    files = files == null ? List.of() : List.copyOf(files);
  }

  /** The state file of the project in the tree, if tib ran on it before. */
  public static Optional<State> in(Tree tree) {
    return tree.read(PATH).map(State::parse);
  }

  /**
   * The state file of the project in the tree; a tree without one is not a tib project.
   *
   * @throws StateException when there is no state file
   */
  public static State of(Tree tree) {
    return in(tree)
        .orElseThrow(
            () -> new StateException(tree.root() + " is not a tib project: there is no " + PATH));
  }

  /**
   * The files that tib wrote and that are no longer as it wrote them, as far as that matters to
   * tib, in the order of the state: a file of the generator's that was edited or is missing, a
   * shared file that is missing, and a file of the developer's that was removed. A file of the
   * developer's or a shared file that was edited is as it should be; only the files of the
   * generator's are read.
   */
  public List<Difference> differences(Tree tree) {
    List<Difference> differences = new ArrayList<>();
    for (File file : files) {
      if (file.owner() == Owner.GENERATOR) {
        Optional<String> content = tree.read(file.path());
        if (content.isEmpty()) {
          differences.add(new Difference(file.path(), Difference.Kind.MISSING));
        } else if (!sha256(content.get()).equals(file.sha256())) {
          differences.add(new Difference(file.path(), Difference.Kind.EDITED));
        }
      } else if (!tree.exists(file.path())) {
        differences.add(
            new Difference(
                file.path(),
                file.owner() == Owner.DEVELOPER
                    ? Difference.Kind.REMOVED
                    : Difference.Kind.MISSING));
      }
    }
    return List.copyOf(differences);
  }

  /** The state as YAML. */
  public String yaml() {
    return YAML.writeValueAsString(this);
  }

  /**
   * The state read from its YAML, with its spec checked as a spec file is; a problem of that spec
   * is a problem of the state file.
   */
  public static State parse(String yaml) {
    State state;
    try {
      state = YAML.readValue(yaml, State.class);
    } catch (JacksonException e) {
      throw new StateException(PATH + ": " + e.getOriginalMessage());
    }
    if (state == null || state.spec() == null) {
      throw new StateException(PATH + ": spec: required");
    }
    try {
      state.spec().check();
    } catch (SpecException e) {
      throw new StateException(PATH + ": " + e.getMessage());
    }
    return state;
  }

  /** The SHA-256 of a text, in hex. */
  static String sha256(String text) {
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  /** The SHA-256 of bytes, in hex. */
  static String sha256(byte[] bytes) {
    return digest("SHA-256", bytes);
  }

  static String digest(String algorithm, byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
