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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import me.yakar.tib.state.State;
import me.yakar.tib.tree.Owner;
import me.yakar.tib.tree.ProjectFiles;
import me.yakar.tib.tree.Tree;

/**
 * The files of a tree as a generator writes them, with a record of what it generated, whether that
 * staged a change or not.
 */
final class Recorder implements ProjectFiles {

  private final Tree tree;
  private final SortedSet<String> generated = new TreeSet<>();
  private final SortedMap<String, byte[]> lasting = new TreeMap<>();
  private final SortedMap<String, State.File> written = new TreeMap<>();

  Recorder(Tree tree) {
    this.tree = tree;
  }

  /**
   * The paths of the files that the generator owns or writes once, as the tree names them, in
   * order; the files that last are not among them.
   */
  SortedSet<String> generated() {
    return generated;
  }

  /** The files that last, by path as the tree names it, with the content that was generated. */
  SortedMap<String, byte[]> lasting() {
    return lasting;
  }

  /**
   * The files that are, in the tree, as the generator made them, staged or there already, in the
   * order of their paths: the ones that tib has to have on record.
   */
  Collection<State.File> written() {
    return written.values();
  }

  @Override
  public void own(String path, String content) {
    tree.own(path, content);
    generated.add(tree.key(path));
    record(path, Owner.GENERATOR, utf8(content));
  }

  @Override
  public void once(String path, String content) {
    once(path, utf8(content), false);
  }

  @Override
  public void once(String path, byte[] content, boolean executable) {
    tree.once(path, content, executable);
    generated.add(tree.key(path));
    record(path, Owner.DEVELOPER, content);
  }

  @Override
  public void lasting(String path, String content) {
    tree.lasting(path, content);
    lasting.put(tree.key(path), utf8(content));
    record(path, Owner.DEVELOPER, utf8(content));
  }

  private void record(String path, Owner owner, byte[] content) {
    if (tree.bytes(path).filter(there -> Arrays.equals(there, content)).isPresent()) {
      written.put(tree.key(path), State.File.of(tree.key(path), owner, content));
    }
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
