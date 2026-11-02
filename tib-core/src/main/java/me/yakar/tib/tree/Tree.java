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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

/**
 * The files of a project as a run of tib sees them: the directory on disk, with the changes of the
 * run staged on top. Nothing reaches the disk until {@link #apply()}, so a dry run reads the same
 * tree, prints its {@link #diff()} and stops. A change that leaves a file as it is stages nothing,
 * so a second run of the same generation stages nothing.
 */
public final class Tree {

  private final Path root;
  private final SortedMap<String, Change> changes = new TreeMap<>();
  private final SortedSet<String> writtenBefore = new TreeSet<>();

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

  /** Whether the file is there, as the run sees it; it is not read. */
  public boolean exists(String path) {
    String key = key(path);
    return changes.containsKey(key) || Files.isRegularFile(root.resolve(key));
  }

  /** The paths of the files under the directory, at any depth, on disk or staged, in order. */
  public List<String> list(String directory) {
    String prefix = key(directory) + "/";
    SortedSet<String> paths = new TreeSet<>();
    list(root.resolve(prefix).toFile(), prefix, paths);
    changes.keySet().stream().filter(path -> path.startsWith(prefix)).forEach(paths::add);
    return List.copyOf(paths);
  }

  /**
   * Adds the files under the directory on disk. Through File: a directory that is not there, or
   * cannot be read, has no files, and no exception.
   */
  private static void list(File directory, String prefix, SortedSet<String> paths) {
    for (File file : Objects.requireNonNullElse(directory.listFiles(), new File[0])) {
      if (file.isFile()) {
        paths.add(prefix + file.getName());
      } else {
        list(file, prefix + file.getName() + "/", paths);
      }
    }
  }

  /** Stages a file that the generator owns, whatever is there. */
  public void own(String path, String content) {
    stage(key(path), content, Owner.GENERATOR);
  }

  /**
   * Stages a file that the developer owns, only when there is none and tib did not write it before:
   * a file that the developer removed stays removed.
   */
  public void once(String path, String content) {
    String key = key(path);
    if (!writtenBefore.contains(key) && !exists(key)) {
      stage(key, content, Owner.DEVELOPER);
    }
  }

  /** Stages an edit of a shared file; the file has to be there. */
  public void edit(String path, UnaryOperator<String> edit) {
    String key = key(path);
    String content = read(key).orElseThrow(() -> new TreeException(key + " is not there to edit"));
    stage(key, edit.apply(content), Owner.SHARED);
  }

  /** Tells the tree the files that tib wrote to the project in earlier runs. */
  public void writtenBefore(Collection<String> paths) {
    paths.forEach(path -> writtenBefore.add(key(path)));
  }

  /** The paths under the directory, at any depth, that tib wrote in earlier runs, there or not. */
  public List<String> recorded(String directory) {
    String prefix = key(directory) + "/";
    return writtenBefore.stream().filter(path -> path.startsWith(prefix)).toList();
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
   * Writes the staged changes to disk. Every file is first written next to its target, and then
   * moved over it in one step, so a file is either the old one or the new one, never half written;
   * when a write fails, the files written next to their targets are removed. Afterwards nothing is
   * staged.
   */
  public void apply() {
    // The files written next to their targets, by target, in the order of the paths.
    Map<Path, Path> staged = new LinkedHashMap<>();
    // What is written when it fails: the directory of a file, the file, or the file it replaces.
    Path target = root;
    try {
      for (Change change : changes.values()) {
        Path file = root.resolve(change.path());
        target = file.getParent();
        Files.createDirectories(target);
        target = file;
        Path staging = Files.createTempFile(file.getParent(), ".tib-", ".tmp");
        staged.put(file, staging);
        Files.writeString(staging, change.after());
      }
      for (Map.Entry<Path, Path> entry : staged.entrySet()) {
        target = entry.getKey();
        Files.move(
            entry.getValue(),
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      // Through File: a file that was moved already, or cannot be removed, is no exception.
      staged.values().forEach(staging -> staging.toFile().delete());
      throw new TreeException("write", target, e);
    }
    changes.clear();
  }

  private void stage(String key, String after, Owner owner) {
    String before = onDisk(key).orElse(null);
    if (after.equals(before)) {
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
      throw new TreeException("read", file, e);
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
