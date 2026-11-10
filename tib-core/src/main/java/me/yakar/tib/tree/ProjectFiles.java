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

import java.util.List;
import java.util.Optional;

/**
 * The files of a project as a generator sees and writes them: it reads what is there and stages
 * what it generates, each file with its owner. It neither deletes a file nor writes to the disk.
 */
public interface ProjectFiles {

  /** Stages a file that the generator owns, whatever is there. */
  void own(String path, String content);

  /**
   * Stages a file that the developer owns, only when there is none and tib did not write it before:
   * a file that the developer removed stays removed.
   */
  void once(String path, String content);

  /**
   * Stages a file that the developer owns as {@link #once(String, String)} does, with content that
   * need not be text, and that is written as one that can be run if it is executable, where the
   * file system can say so.
   */
  void once(String path, byte[] content, boolean executable);

  /**
   * Stages a file that the developer owns as {@link #once(String, String)} does, but one that
   * lasts: tib does not delete it when the spec no longer generates it, as a migration that a
   * database may have run.
   */
  void lasting(String path, String content);

  /**
   * The content of the file as text, as the run sees it: staged, or else on disk.
   *
   * @throws TreeException when the file is not UTF-8 text
   */
  Optional<String> read(String path);

  /** Whether the file is there, as the run sees it; it is not read. */
  boolean exists(String path);

  /** The paths of the files under the directory, at any depth, on disk or staged, in order. */
  List<String> list(String directory);

  /** The paths under the directory, at any depth, that tib wrote in earlier runs, there or not. */
  List<String> recorded(String directory);
}
