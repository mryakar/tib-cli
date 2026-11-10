// Written by tib on every run. Do not edit: your changes would be lost.
package me.yakar.cornerservice.item;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An item as the API shows and takes it; a relation is the identifier of its target. The identifier
 * is read only; the service ignores it.
 */
public record ItemDto(Long id, @Size(max = 255) String label, @NotNull Long branchId) {}
