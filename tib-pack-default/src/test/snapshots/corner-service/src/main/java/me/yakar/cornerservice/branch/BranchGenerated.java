// Written by tib on every run. Do not edit: your changes would be lost. Your code goes in Branch.
package me.yakar.cornerservice.branch;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** The columns of BRANCH, as the spec names them; {@link Branch} is the entity. */
@MappedSuperclass
public abstract class BranchGenerated {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "branch")
  @SequenceGenerator(name = "branch", sequenceName = "BRANCH_SEQ", allocationSize = 50)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "CODE", nullable = false)
  private String code;

  @Column(name = "COUNT")
  private Long count;

  @Column(name = "OPEN")
  private Boolean open;

  @Column(name = "OPENED_AT")
  private LocalDateTime openedAt;

  @Column(name = "SEEN_AT")
  private Instant seenAt;

  @Column(name = "TOKEN")
  private UUID token;

  @Column(name = "RATIO")
  private BigDecimal ratio;

  @Column(name = "AMOUNT", precision = 10)
  private BigDecimal amount;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_ID", unique = true)
  private Branch parent;

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

  public Branch getParent() {
    return parent;
  }

  public void setParent(Branch parent) {
    this.parent = parent;
  }
}
