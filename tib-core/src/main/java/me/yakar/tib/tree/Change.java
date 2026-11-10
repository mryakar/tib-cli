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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/**
 * A change that a run of tib stages for one file: its content before, null when the file is new,
 * and after, null when the file is deleted, as bytes; and whether the file is written as one that
 * can be run. The bytes are compared by identity: compare a change by its parts.
 */
public record Change(String path, byte[] before, byte[] after, Owner owner, boolean executable) {

  /** Whether the change deletes the file. */
  public boolean deletes() {
    return after == null;
  }

  /**
   * The change as a unified diff; a file that is not text, as git tells, shows as one line that
   * says it differs.
   */
  public String diff() {
    return diff(path, before, after);
  }

  /**
   * The unified diff of the file at the path, from its content before, null when there is none, to
   * its content after, null when there is none; a file that is not text shows as one line.
   */
  public static String diff(String path, byte[] before, byte[] after) {
    if (binary(before) || binary(after)) {
      return "Binary files "
          + (before == null ? "/dev/null" : "a/" + path)
          + " and "
          + (after == null ? "/dev/null" : "b/" + path)
          + " differ\n";
    }
    return UnifiedDiff.of(path, text(before), text(after));
  }

  /** Whether the content is not text: it has a zero byte, or is not UTF-8. */
  static boolean binary(byte[] content) {
    if (content == null) {
      return false;
    }
    for (byte b : content) {
      if (b == 0) {
        return true;
      }
    }
    try {
      decode(content);
      return false;
    } catch (CharacterCodingException e) {
      return true;
    }
  }

  /** The UTF-8 text of the content, which has to be text. */
  static String decode(byte[] content) throws CharacterCodingException {
    return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(content)).toString();
  }

  private static String text(byte[] content) {
    return content == null ? null : new String(content, StandardCharsets.UTF_8);
  }
}
