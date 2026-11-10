// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sample branches for the tests, with the first or the second sample values: a test changes the
 * value it is about through the builder.
 */
public final class BranchSamples {

  private BranchSamples() {}

  /** A branch with the first sample values. */
  public static BranchBuilder branch() {
    return new BranchBuilder()
        .code("One")
        .count(1L)
        .open(true)
        .openedAt(LocalDateTime.of(2026, 1, 1, 12, 0))
        .seenAt(Instant.parse("2026-01-01T12:00:00Z"))
        .token(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        .ratio(new BigDecimal("1"))
        .amount(new BigDecimal("1"));
  }

  /** A branch with the second sample values. */
  public static BranchBuilder otherBranch() {
    return new BranchBuilder()
        .code("Two")
        .count(2L)
        .open(false)
        .openedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
        .seenAt(Instant.parse("2026-01-02T12:00:00Z"))
        .token(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        .ratio(new BigDecimal("2"))
        .amount(new BigDecimal("2"));
  }

  /** A branch as the API takes it, with the first sample values. */
  public static BranchDtoBuilder dto() {
    return new BranchDtoBuilder()
        .code("One")
        .count(1L)
        .open(true)
        .openedAt(LocalDateTime.of(2026, 1, 1, 12, 0))
        .seenAt(Instant.parse("2026-01-01T12:00:00Z"))
        .token(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        .ratio(new BigDecimal("1"))
        .amount(new BigDecimal("1"));
  }

  /** A branch as the API takes it, with the second sample values. */
  public static BranchDtoBuilder otherDto() {
    return new BranchDtoBuilder()
        .code("Two")
        .count(2L)
        .open(false)
        .openedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
        .seenAt(Instant.parse("2026-01-02T12:00:00Z"))
        .token(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        .ratio(new BigDecimal("2"))
        .amount(new BigDecimal("2"));
  }

  /** A branch as the API takes it, without any value. */
  public static BranchDtoBuilder emptyDto() {
    return new BranchDtoBuilder();
  }

  /** A branch to build, one value at a time. */
  public static final class BranchBuilder {
    private String code;
    private Long count;
    private Boolean open;
    private LocalDateTime openedAt;
    private Instant seenAt;
    private UUID token;
    private BigDecimal ratio;
    private BigDecimal amount;
    private Branch parent;

    private BranchBuilder() {}

    public BranchBuilder code(String code) {
      this.code = code;
      return this;
    }

    public BranchBuilder count(Long count) {
      this.count = count;
      return this;
    }

    public BranchBuilder open(Boolean open) {
      this.open = open;
      return this;
    }

    public BranchBuilder openedAt(LocalDateTime openedAt) {
      this.openedAt = openedAt;
      return this;
    }

    public BranchBuilder seenAt(Instant seenAt) {
      this.seenAt = seenAt;
      return this;
    }

    public BranchBuilder token(UUID token) {
      this.token = token;
      return this;
    }

    public BranchBuilder ratio(BigDecimal ratio) {
      this.ratio = ratio;
      return this;
    }

    public BranchBuilder amount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public BranchBuilder parent(Branch parent) {
      this.parent = parent;
      return this;
    }

    public Branch build() {
      Branch branch = new Branch();
      branch.setCode(code);
      branch.setCount(count);
      branch.setOpen(open);
      branch.setOpenedAt(openedAt);
      branch.setSeenAt(seenAt);
      branch.setToken(token);
      branch.setRatio(ratio);
      branch.setAmount(amount);
      branch.setParent(parent);
      return branch;
    }
  }

  /** A branch as the API takes it, to build one value at a time; its audit times are none. */
  public static final class BranchDtoBuilder {
    private Long id;
    private String code;
    private Long count;
    private Boolean open;
    private LocalDateTime openedAt;
    private Instant seenAt;
    private UUID token;
    private BigDecimal ratio;
    private BigDecimal amount;
    private Long parentId;

    private BranchDtoBuilder() {}

    public BranchDtoBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public BranchDtoBuilder code(String code) {
      this.code = code;
      return this;
    }

    public BranchDtoBuilder count(Long count) {
      this.count = count;
      return this;
    }

    public BranchDtoBuilder open(Boolean open) {
      this.open = open;
      return this;
    }

    public BranchDtoBuilder openedAt(LocalDateTime openedAt) {
      this.openedAt = openedAt;
      return this;
    }

    public BranchDtoBuilder seenAt(Instant seenAt) {
      this.seenAt = seenAt;
      return this;
    }

    public BranchDtoBuilder token(UUID token) {
      this.token = token;
      return this;
    }

    public BranchDtoBuilder ratio(BigDecimal ratio) {
      this.ratio = ratio;
      return this;
    }

    public BranchDtoBuilder amount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public BranchDtoBuilder parentId(Long parentId) {
      this.parentId = parentId;
      return this;
    }

    public BranchDto build() {
      return new BranchDto(
          id, code, count, open, openedAt, seenAt, token, ratio, amount, parentId, null, null);
    }
  }
}
