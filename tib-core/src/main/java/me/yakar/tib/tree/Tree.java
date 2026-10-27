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
package me.yakar.tib.tree;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * The files of a project as a run of tib sees them: the directory on disk, with the changes of the
 * run staged on top. Nothing reaches the disk until {@link #apply()}, so a dry run reads the same
 * tree, prints its {@link #diff()} and stops. A change that leaves a file as it is, is no change: a
 * second run of the same generation stages nothing.
 */
public final class Tree {

  private final Path root;
  private final SortedMap<String, Change> changes = new TreeMap<>();

  private Tree(Path root) {
    this.root = root;
  }

  /** The tree of the project at the directory. */
  public static Tree at(Path root) {
    return new Tree(root.toAbsolutePath().normalize());
  }

  public Path root() {
    return root;
  }

  /** The content of the file, as the run sees it: staged, or else on disk. */
  public Optional<String> read(String path) {
    String key = key(path);
    Change change = changes.get(key);
    return change != null ? Optional.of(change.after()) : onDisk(key);
  }

  public boolean exists(String path) {
    return read(path).isPresent();
  }

  /** Writes a file of the generator's, whatever is there. */
  public void own(String path, String content) {
    stage(key(path), content, Owner.GENERATOR);
  }

  /** Writes a file of the developer's, only when there is none. */
  public void once(String path, String content) {
    String key = key(path);
    if (!exists(key)) {
      stage(key, content, Owner.DEVELOPER);
    }
  }

  /** Edits a shared file in place; it has to be there. */
  public void edit(String path, UnaryOperator<String> edit) {
    String key = key(path);
    String content = read(key).orElseThrow(() -> new TreeException(key + " is not there to edit"));
    stage(key, edit.apply(content), Owner.SHARED);
  }

  /** The changes staged so far, by path. */
  public List<Change> changes() {
    return List.copyOf(changes.values());
  }

  /** The staged changes as one unified diff; empty when there are none. */
  public String diff() {
    StringBuilder diff = new StringBuilder();
    changes.values().forEach(change -> diff.append(change.diff()));
    return diff.toString();
  }

  /**
   * Writes the staged changes to disk, each file in one step: a file is either the old one or the
   * new one, never half written. Afterwards nothing is staged.
   */
  public void apply() {
    try {
      for (Change change : changes.values()) {
        Path file = root.resolve(change.path());
        Files.createDirectories(file.getParent());
        Path staging = Files.createTempFile(file.getParent(), ".tib-", ".tmp");
        Files.writeString(staging, change.after());
        Files.move(
            staging, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    changes.clear();
  }

  private void stage(String key, String after, Owner owner) {
    Optional<String> before = onDisk(key);
    if (before.isPresent() && before.get().equals(after)) {
      changes.remove(key);
    } else {
      changes.put(key, new Change(key, before, after, owner));
    }
  }

  private Optional<String> onDisk(String key) {
    Path file = root.resolve(key);
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readString(file));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The path as the tree names it: relative to the root, normalized, with forward slashes. */
  private String key(String path) {
    Path resolved = root.resolve(path).normalize();
    if (!resolved.startsWith(root) || resolved.equals(root)) {
      throw new TreeException(path + " is outside the project");
    }
    StringBuilder key = new StringBuilder();
    for (Path name : root.relativize(resolved)) {
      if (!key.isEmpty()) {
        key.append('/');
      }
      key.append(name);
    }
    return key.toString();
  }
}
