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
package me.yakar.tib.generate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Owner;
import me.yakar.tib.tree.ProjectFiles;
import me.yakar.tib.tree.Tree;
import me.yakar.tib.tree.TreeException;

/**
 * The files of a tree as a generator writes them, with a record of what it generated, whether that
 * staged a change or not.
 */
final class Recorder implements ProjectFiles {

  /** A file as the generator made it. */
  private record Made(Owner owner, byte[] content, boolean lasts) {}

  private final Tree tree;
  private final SortedMap<String, Made> made = new TreeMap<>();

  Recorder(Tree tree) {
    this.tree = tree;
  }

  /**
   * The paths of the files that the generator owns or writes once, as the tree names them; the
   * files that last are not among them.
   */
  Set<String> generated() {
    return made.entrySet().stream()
        .filter(file -> !file.getValue().lasts())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  /** The files that last, by path as the tree names it, with the content that was generated. */
  SortedMap<String, byte[]> lasting() {
    SortedMap<String, byte[]> lasting = new TreeMap<>();
    made.forEach(
        (path, file) -> {
          if (file.lasts()) {
            lasting.put(path, file.content());
          }
        });
    return lasting;
  }

  /**
   * The files that are, in the tree, as the generator made them, staged or there already, in the
   * order of their paths: the ones that tib has to have on record. Every file that the generator
   * made and that is there is read to tell.
   *
   * @throws TreeException when a file that is there cannot be read: that ends the run
   */
  List<State.File> written() {
    List<State.File> written = new ArrayList<>();
    made.forEach(
        (path, file) -> {
          if (tree.bytes(path).filter(there -> Arrays.equals(there, file.content())).isPresent()) {
            written.add(State.File.of(path, file.owner(), file.content()));
          }
        });
    return written;
  }

  @Override
  public void own(String path, String content) {
    tree.own(path, content);
    made.put(tree.key(path), new Made(Owner.GENERATOR, utf8(content), false));
  }

  @Override
  public void once(String path, String content) {
    once(path, utf8(content), false);
  }

  @Override
  public void once(String path, byte[] content, boolean executable) {
    tree.once(path, content, executable);
    made.put(tree.key(path), new Made(Owner.DEVELOPER, content, false));
  }

  @Override
  public void lasting(String path, String content) {
    tree.lasting(path, content);
    made.put(tree.key(path), new Made(Owner.DEVELOPER, utf8(content), true));
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Optional<String> read(String path) {
    return tree.read(path);
  }

  @Override
  public boolean exists(String path) {
    return tree.exists(path);
  }

  @Override
  public List<String> list(String directory) {
    return tree.list(directory);
  }

  @Override
  public List<String> recorded(String directory) {
    return tree.recorded(directory);
  }
}
