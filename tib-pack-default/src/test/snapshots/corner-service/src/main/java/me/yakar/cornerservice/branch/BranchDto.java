// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.branch;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A branch as the API shows and takes it; a relation is the identifier of its target. The
 * identifier and the audit times are read only; the service ignores them.
 */
public record BranchDto(
    Long id,
    @NotNull @Size(max = 255) String code,
    Long count,
    Boolean open,
    LocalDateTime openedAt,
    Instant seenAt,
    UUID token,
    BigDecimal ratio,
    BigDecimal amount,
    Long parentId,
    Instant createdAt,
    Instant updatedAt) {}
