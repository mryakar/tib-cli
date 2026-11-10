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
package me.yakar.tib;

import java.io.IOException;
import java.io.Serial;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;

/**
 * Input that tib cannot use, or a file that it cannot read or write. The message is for the user:
 * every failure that the user can fix is one of these.
 */
public abstract class TibException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  // An immutable list, which serializes; the lint cannot see that.
  @SuppressWarnings("serial")
  private final List<String> problems;

  /** The problems of the subject, as in {@code the spec has a problem: tib: required}. */
  protected TibException(String subject, List<String> problems) {
    super(message(subject, problems));
    this.problems = List.copyOf(problems);
  }

  /** One problem, which is the message. */
  protected TibException(String message) {
    this(message, (Throwable) null);
  }

  /** A file that cannot be read or written, as in {@code cannot read spec.yaml: no such file}. */
  protected TibException(String verb, Path file, IOException cause) {
    this("cannot " + verb + " " + file + ": " + reason(cause), cause);
  }

  private TibException(String message, Throwable cause) {
    super(message, cause);
    this.problems = List.of(message);
  }

  /** The problems, each with what it is about. */
  public List<String> problems() {
    return problems;
  }

  /** Why the file system refused, in words. */
  static String reason(IOException e) {
    return switch (e) {
      case NoSuchFileException missing -> "no such file";
      case AccessDeniedException denied -> "access denied";
      case FileAlreadyExistsException there -> "already exists";
      case NotDirectoryException file -> "not a directory";
      case CharacterCodingException text -> "not UTF-8 text";
      default -> e.getMessage();
    };
  }

  private static String message(String subject, List<String> problems) {
    if (problems.size() == 1) {
      return "the " + subject + " has a problem: " + problems.get(0);
    }
    return "the "
        + subject
        + " has "
        + problems.size()
        + " problems:\n  "
        + String.join("\n  ", problems);
  }
}
