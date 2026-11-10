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
import java.nio.file.FileSystems;
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
import java.util.stream.Stream;

/**
 * The files of a project as a run of tib sees them: the directory on disk, with the changes of the
 * run staged on top. Nothing reaches the disk until {@link #apply()}, so a dry run reads the same
 * tree, prints its {@link #diff()} and stops. A change that leaves a file as it is stages nothing,
 * so a second run of the same generation stages nothing.
 */
public final class Tree {

  /** The attributes of a file that the file replacing it keeps, of those the file system has. */
  private static final List<String> KEPT =
      Stream.of("posix:permissions").filter(Tree::supported).toList();

  private final Path root;
  private final SortedMap<String, Change> changes = new TreeMap<>();
  private final SortedSet<String> writtenBefore = new TreeSet<>();
  private final SortedSet<String> generated = new TreeSet<>();
  private final SortedSet<String> deleted = new TreeSet<>();

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
    return change != null ? Optional.ofNullable(change.after()) : onDisk(key);
  }

  /** Whether the file is there, as the run sees it; it is not read. */
  public boolean exists(String path) {
    String key = key(path);
    Change change = changes.get(key);
    return change != null ? !change.deletes() : Files.isRegularFile(root.resolve(key));
  }

  /** The paths of the files under the directory, at any depth, on disk or staged, in order. */
  public List<String> list(String directory) {
    String prefix = key(directory) + "/";
    SortedSet<String> paths = new TreeSet<>();
    list(root.resolve(prefix).toFile(), prefix, paths);
    for (Change change : changes.values()) {
      if (!change.path().startsWith(prefix)) {
        continue;
      }
      if (change.deletes()) {
        paths.remove(change.path());
      } else {
        paths.add(change.path());
      }
    }
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
    String key = key(path);
    generated.add(key);
    stage(key, content, Owner.GENERATOR);
  }

  /**
   * Stages a file that the developer owns, only when there is none and tib did not write it before:
   * a file that the developer removed stays removed.
   */
  public void once(String path, String content) {
    String key = key(path);
    generated.add(key);
    stageOnce(key, content);
  }

  /**
   * Stages a file that the developer owns as {@link #once} does, but one that lasts: it is not
   * among the {@link #generated()} files, so tib does not delete it when the spec no longer
   * generates it, as a migration that a database may have run.
   */
  public void lasting(String path, String content) {
    stageOnce(key(path), content);
  }

  private void stageOnce(String key, String content) {
    if (!writtenBefore.contains(key) && !exists(key)) {
      stage(key, content, Owner.DEVELOPER);
    }
  }

  /**
   * The paths of the files that the generator owns or writes once and that were staged in this
   * tree, in order, whether that changed them or not; the files that last are not among them.
   */
  public List<String> generated() {
    return List.copyOf(generated);
  }

  /**
   * Stages the deletion of a file of the owner's that tib no longer generates. A file that is not
   * there stages nothing, and is deleted all the same.
   */
  public void delete(String path, Owner owner) {
    String key = key(path);
    deleted.add(key);
    onDisk(key).ifPresent(before -> changes.put(key, new Change(key, before, null, owner)));
  }

  /** The paths of the files that were deleted in this tree, there or not, in order. */
  public List<String> deleted() {
    return List.copyOf(deleted);
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
   * Writes the staged changes to disk. Every file is first written next to its target, as {@code
   * <name>.tib.tmp} with the permissions of a new file or of the file it replaces, and then moved
   * over it in one step, so a file is either the old one or the new one, never half written; when a
   * write fails, the files written next to their targets are removed. Then the files to delete are
   * deleted, and with them every directory that this leaves empty, up to the root. Afterwards
   * nothing is staged.
   */
  public void apply() {
    // The files written next to their targets, by target, in the order of the paths.
    Map<Path, Path> staged = new LinkedHashMap<>();
    // What is written when it fails: the directory of a file, the file, or the file it replaces.
    Path target = root;
    try {
      for (Change change : changes.values()) {
        if (change.deletes()) {
          continue;
        }
        Path file = root.resolve(change.path());
        target = file.getParent();
        Files.createDirectories(target);
        target = file;
        Path staging = file.resolveSibling(file.getFileName() + ".tib.tmp");
        Files.deleteIfExists(staging);
        Files.createFile(staging);
        staged.put(file, staging);
        Files.writeString(staging, change.after());
        keep(file, staging, KEPT);
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
    try {
      for (Change change : changes.values()) {
        if (change.deletes()) {
          target = root.resolve(change.path());
          Files.deleteIfExists(target);
          for (target = target.getParent();
              !target.equals(root) && empty(target);
              target = target.getParent()) {
            Files.delete(target);
          }
        }
      }
    } catch (IOException e) {
      throw new TreeException("delete", target, e);
    }
    changes.clear();
  }

  private static boolean empty(Path directory) throws IOException {
    try (Stream<Path> files = Files.list(directory)) {
      return files.findAny().isEmpty();
    }
  }

  /** Gives the file written next to its target the attributes of the target, if it is there. */
  static void keep(Path file, Path staging, List<String> attributes) throws IOException {
    if (Files.isRegularFile(file)) {
      for (String attribute : attributes) {
        Files.setAttribute(staging, attribute, Files.getAttribute(file, attribute));
      }
    }
  }

  private static boolean supported(String attribute) {
    String view = attribute.substring(0, attribute.indexOf(':'));
    return FileSystems.getDefault().supportedFileAttributeViews().contains(view);
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
