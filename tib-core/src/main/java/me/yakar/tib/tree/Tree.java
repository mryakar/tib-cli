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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
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
 * run staged on top. Nothing reaches the disk until the tree is applied, so a dry run reads the
 * same tree, prints its {@link #diff()} and stops. A change that leaves a file as it is stages
 * nothing, so a second run of the same generation stages nothing.
 */
public final class Tree implements ProjectFiles {

  /** The attributes of a file that the file replacing it keeps, of those the file system has. */
  private static final List<String> KEPT =
      Stream.of("posix:permissions").filter(Tree::supported).toList();

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

  @Override
  public Optional<String> read(String path) {
    String key = key(path);
    Optional<byte[]> content = bytes(key);
    try {
      return content.isEmpty() ? Optional.empty() : Optional.of(Change.decode(content.get()));
    } catch (CharacterCodingException e) {
      throw new TreeException("read", root.resolve(key), e);
    }
  }

  /** The content of the file as bytes, as the run sees it: staged, or else on disk. */
  public Optional<byte[]> bytes(String path) {
    String key = key(path);
    Change change = changes.get(key);
    return change != null ? Optional.ofNullable(change.after()) : onDisk(key);
  }

  @Override
  public boolean exists(String path) {
    String key = key(path);
    Change change = changes.get(key);
    return change != null ? !change.deletes() : Files.isRegularFile(root.resolve(key));
  }

  @Override
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

  @Override
  public void own(String path, String content) {
    stage(key(path), utf8(content), Owner.GENERATOR, false);
  }

  @Override
  public void once(String path, String content) {
    once(path, utf8(content), false);
  }

  @Override
  public void once(String path, byte[] content, boolean executable) {
    String key = key(path);
    if (!writtenBefore.contains(key) && !exists(key)) {
      stage(key, content, Owner.DEVELOPER, executable);
    }
  }

  /**
   * Stages the file as {@link #once(String, String)} does: that it lasts is for the run to know.
   */
  @Override
  public void lasting(String path, String content) {
    once(path, content);
  }

  /**
   * Stages the deletion of a file of the owner's, if it is there; a file that is not there has no
   * change.
   */
  public void delete(String path, Owner owner) {
    String key = key(path);
    Optional<byte[]> before = onDisk(key);
    if (before.isPresent()) {
      changes.put(key, new Change(key, before.get(), null, owner, false));
    } else {
      changes.remove(key);
    }
  }

  /** Stages an edit of a shared file; the file has to be there. */
  public void edit(String path, UnaryOperator<String> edit) {
    String key = key(path);
    String content = read(key).orElseThrow(() -> new TreeException(key + " is not there to edit"));
    stage(key, utf8(edit.apply(content)), Owner.SHARED, false);
  }

  /** Tells the tree the files that tib wrote to the project in earlier runs. */
  public void writtenBefore(Collection<String> paths) {
    paths.forEach(path -> writtenBefore.add(key(path)));
  }

  @Override
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
   * Writes the staged changes to disk, as {@link #apply(String)} does, in the order of the paths.
   */
  public void apply() {
    write(null);
  }

  /**
   * Writes the staged changes to disk. Every file is first written next to its target, as {@code
   * <name>.tib.tmp} with the permissions of a new file, which can be run if the change says so, or
   * of the file it replaces, and then moved over it in one step, so a file is either the old one or
   * the new one, never half written. Then the files to delete are deleted, and with them every
   * directory that this leaves empty, up to the root, as far as the file system lets them go. The
   * file at the last path is moved into place after all of that, so a file that records the run is
   * the old one as long as the run did not come through. When a write or a deletion fails, the
   * files written next to their targets are removed. Afterwards nothing is staged.
   *
   * @param last the path of the file that is moved into place last, if it is staged
   */
  public void apply(String last) {
    write(key(last));
  }

  private void write(String last) {
    // The files written next to their targets, by target, in the order of the paths.
    Map<Path, Path> staged = new LinkedHashMap<>();
    String verb = "write";
    // What is written or deleted when it fails: a directory, a file, or the file it replaces.
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
        Files.write(staging, change.after());
        if (change.executable()) {
          // Through File: a file system without the permission to run a file ignores it.
          staging.toFile().setExecutable(true, false);
        }
        keep(file, staging, KEPT);
      }
      Path lastFile = last == null ? null : root.resolve(last);
      for (Map.Entry<Path, Path> entry : staged.entrySet()) {
        if (!entry.getKey().equals(lastFile)) {
          target = entry.getKey();
          move(entry.getValue(), target);
        }
      }
      verb = "delete";
      for (Change change : changes.values()) {
        if (change.deletes()) {
          target = root.resolve(change.path());
          Files.deleteIfExists(target);
          // Through File: a directory that is not empty, or cannot be deleted, stays.
          Path directory = target.getParent();
          while (!directory.equals(root) && directory.toFile().delete()) {
            directory = directory.getParent();
          }
        }
      }
      if (staged.containsKey(lastFile)) {
        verb = "write";
        target = lastFile;
        move(staged.get(lastFile), target);
      }
    } catch (IOException e) {
      // Through File: a file that was moved already, or cannot be removed, is no exception.
      staged.values().forEach(staging -> staging.toFile().delete());
      throw new TreeException(verb, target, e);
    }
    changes.clear();
  }

  private static void move(Path staging, Path target) throws IOException {
    Files.move(
        staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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

  private void stage(String key, byte[] after, Owner owner, boolean executable) {
    byte[] before = onDisk(key).orElse(null);
    if (Arrays.equals(after, before)) {
      changes.remove(key);
    } else {
      changes.put(key, new Change(key, before, after, owner, executable));
    }
  }

  private Optional<byte[]> onDisk(String key) {
    Path file = root.resolve(key);
    return Files.isRegularFile(file) ? Optional.of(read(file)) : Optional.empty();
  }

  /** The bytes of the file. */
  static byte[] read(Path file) {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new TreeException("read", file, e);
    }
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * The path as the tree names it: relative to the root, normalized, with forward slashes.
   *
   * @throws TreeException when the path is outside the project
   */
  public String key(String path) {
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
