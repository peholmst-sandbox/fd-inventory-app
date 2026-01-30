package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a fire apparatus.
 *
 * <p>An apparatus is a fire service vehicle (fire engine, ladder truck, rescue unit, etc.)
 * that carries equipment and responds to incidents. Each apparatus has compartments
 * containing equipment that must be tracked and verified.
 *
 * @param value the UUID value, must not be null
 */
public record ApparatusId(UUID value) {
    public ApparatusId {
        Objects.requireNonNull(value, "Apparatus ID cannot be null");
    }

    /**
     * Generates a new apparatus ID with a random UUID.
     *
     * @return a new apparatus ID
     */
    public static ApparatusId generate() {
        return new ApparatusId(UUID.randomUUID());
    }

    /**
     * Creates an apparatus ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an apparatus ID wrapping the given UUID
     */
    public static ApparatusId of(UUID value) {
        return new ApparatusId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
