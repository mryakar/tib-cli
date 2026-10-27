// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.tag;

import jakarta.validation.constraints.NotNull;

/** A tag as the API shows and takes it; a relation is the identifier of its target. */
public record TagDto(
    Long id,
    String label,
    @NotNull Long branchId,
    Long otherId) {}
