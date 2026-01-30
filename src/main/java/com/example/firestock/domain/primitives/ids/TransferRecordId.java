package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a transfer record.
 *
 * <p>A transfer record documents the movement of equipment items between locations
 * (apparatus, stations, or storage). It provides an audit trail of equipment
 * custody and enables tracking of equipment history.
 *
 * @param value the UUID value, must not be null
 */
public record TransferRecordId(UUID value) {
    public TransferRecordId {
        Objects.requireNonNull(value, "Transfer record ID cannot be null");
    }

    /**
     * Generates a new transfer record ID with a random UUID.
     *
     * @return a new transfer record ID
     */
    public static TransferRecordId generate() {
        return new TransferRecordId(UUID.randomUUID());
    }

    /**
     * Creates a transfer record ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a transfer record ID wrapping the given UUID
     */
    public static TransferRecordId of(UUID value) {
        return new TransferRecordId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
