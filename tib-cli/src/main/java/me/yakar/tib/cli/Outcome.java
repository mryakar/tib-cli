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
package me.yakar.tib.cli;

import java.io.PrintWriter;
import me.yakar.tib.tree.Tree;

/** The end of a run that staged files in a tree: the diff of a dry run, or the files written. */
final class Outcome {

  private Outcome() {}

  /** Prints the diff and stops, or applies the tree and says what was written; exit code 0. */
  static int report(Tree tree, boolean dryRun, PrintWriter out) {
    if (dryRun) {
      out.print(tree.diff());
      out.flush();
      return 0;
    }
    int files = tree.changes().size();
    if (files == 0) {
      out.println("Nothing to change.");
      return 0;
    }
    tree.apply();
    out.println("Wrote " + files + " files to " + tree.root() + ".");
    return 0;
  }
}
