package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a formal audit item record.
 *
 * <p>A formal audit item represents the verification record for a single equipment
 * item or consumable stock entry during a formal audit. It captures condition,
 * test results, and expiry status.
 *
 * @param value the UUID value, must not be null
 */
public record FormalAuditItemId(UUID value) {
    public FormalAuditItemId {
        Objects.requireNonNull(value, "Formal audit item ID cannot be null");
    }

    /**
     * Generates a new formal audit item ID with a random UUID.
     *
     * @return a new formal audit item ID
     */
    public static FormalAuditItemId generate() {
        return new FormalAuditItemId(UUID.randomUUID());
    }

    /**
     * Creates a formal audit item ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a formal audit item ID wrapping the given UUID
     */
    public static FormalAuditItemId of(UUID value) {
        return new FormalAuditItemId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
