package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a restock record.
 *
 * <p>A restock record documents the addition, removal, or adjustment of consumable
 * stock quantities. It provides an audit trail for consumable inventory changes
 * and supports tracking of usage patterns.
 *
 * @param value the UUID value, must not be null
 */
public record RestockRecordId(UUID value) {
    public RestockRecordId {
        Objects.requireNonNull(value, "Restock record ID cannot be null");
    }

    /**
     * Generates a new restock record ID with a random UUID.
     *
     * @return a new restock record ID
     */
    public static RestockRecordId generate() {
        return new RestockRecordId(UUID.randomUUID());
    }

    /**
     * Creates a restock record ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a restock record ID wrapping the given UUID
     */
    public static RestockRecordId of(UUID value) {
        return new RestockRecordId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
