// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag;

import java.util.ArrayList;
import java.util.List;

/**
 * A tag as the API shows and takes it; a relation is the identifier of its target. The identifier
 * is read only; the service ignores it.
 */
public record TagDto(Long id, String label, Long branchId, Long otherId) {

  /**
   * The rules of the spec that the values break, each as the value and the rule; none when they
   * keep them all.
   */
  public List<String> problems() {
    List<String> problems = new ArrayList<>();
    if (label != null && label.length() > 255) {
      problems.add("label: longer than 255 characters");
    }
    if (branchId == null) {
      problems.add("branchId: required");
    }
    return problems;
  }
}
