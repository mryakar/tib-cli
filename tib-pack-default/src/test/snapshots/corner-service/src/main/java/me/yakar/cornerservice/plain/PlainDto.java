// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.plain;

import jakarta.validation.constraints.Size;

/** A plain as the API shows and takes it; a relation is the identifier of its target. */
public record PlainDto(
    Long id,
    @Size(max = 255) String label) {}
