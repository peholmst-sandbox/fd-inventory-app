package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a fire station.
 *
 * <p>A station is a fire service facility where apparatus are housed and firefighters
 * are assigned. It serves as the primary organisational unit for access control and
 * data partitioning in FireStock.
 *
 * @param value the UUID value, must not be null
 */
public record StationId(UUID value) {
    public StationId {
        Objects.requireNonNull(value, "Station ID cannot be null");
    }

    /**
     * Generates a new station ID with a random UUID.
     *
     * @return a new station ID
     */
    public static StationId generate() {
        return new StationId(UUID.randomUUID());
    }

    /**
     * Creates a station ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a station ID wrapping the given UUID
     */
    public static StationId of(UUID value) {
        return new StationId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
