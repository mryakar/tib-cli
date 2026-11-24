// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The columns of BRANCH, as the spec names them, in plain Java; {@link Branch} is the entity, and a
 * relation is the identifier of its target.
 */
public abstract class BranchGenerated {

  private Long id;
  private String code;
  private Long count;
  private Boolean open;
  private LocalDateTime openedAt;
  private Instant seenAt;
  private UUID token;
  private BigDecimal ratio;
  private BigDecimal amount;
  private String memo;
  private Long parentId;
  private Instant createdAt;
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public Boolean getOpen() {
    return open;
  }

  public void setOpen(Boolean open) {
    this.open = open;
  }

  public LocalDateTime getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(LocalDateTime openedAt) {
    this.openedAt = openedAt;
  }

  public Instant getSeenAt() {
    return seenAt;
  }

  public void setSeenAt(Instant seenAt) {
    this.seenAt = seenAt;
  }

  public UUID getToken() {
    return token;
  }

  public void setToken(UUID token) {
    this.token = token;
  }

  public BigDecimal getRatio() {
    return ratio;
  }

  public void setRatio(BigDecimal ratio) {
    this.ratio = ratio;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getMemo() {
    return memo;
  }

  public void setMemo(String memo) {
    this.memo = memo;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
