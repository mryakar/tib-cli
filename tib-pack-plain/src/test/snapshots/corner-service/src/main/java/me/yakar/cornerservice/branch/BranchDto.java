// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A branch as the API shows and takes it; a relation is the identifier of its target. The
 * identifier and the audit times are read only; the service ignores them.
 */
public record BranchDto(
    Long id,
    String code,
    Long count,
    Boolean open,
    LocalDateTime openedAt,
    Instant seenAt,
    UUID token,
    BigDecimal ratio,
    BigDecimal amount,
    String memo,
    Long parentId,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * The rules of the spec that the values break, each as the value and the rule; none when they
   * keep them all.
   */
  public List<String> problems() {
    List<String> problems = new ArrayList<>();
    if (code == null) {
      problems.add("code: required");
    } else if (code.length() > 255) {
      problems.add("code: longer than 255 characters");
    }
    return problems;
  }
}
