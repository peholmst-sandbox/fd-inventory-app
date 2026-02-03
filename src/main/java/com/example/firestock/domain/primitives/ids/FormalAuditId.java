package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a formal audit.
 *
 * <p>A formal audit is a comprehensive equipment inspection performed by maintenance
 * technicians. Unlike shift inventory checks, formal audits include functional tests,
 * condition assessments, and expiry tracking.
 *
 * @param value the UUID value, must not be null
 */
public record FormalAuditId(UUID value) {
    public FormalAuditId {
        Objects.requireNonNull(value, "Formal audit ID cannot be null");
    }

    /**
     * Generates a new formal audit ID with a random UUID.
     *
     * @return a new formal audit ID
     */
    public static FormalAuditId generate() {
        return new FormalAuditId(UUID.randomUUID());
    }

    /**
     * Creates a formal audit ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a formal audit ID wrapping the given UUID
     */
    public static FormalAuditId of(UUID value) {
        return new FormalAuditId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
