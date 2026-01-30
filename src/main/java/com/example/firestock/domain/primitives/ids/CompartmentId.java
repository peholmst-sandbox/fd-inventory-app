package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an apparatus compartment.
 *
 * <p>A compartment is a physical storage area on an apparatus where equipment is kept.
 * Compartments provide organisational structure for equipment and enable systematic
 * inventory checks.
 *
 * @param value the UUID value, must not be null
 */
public record CompartmentId(UUID value) {
    public CompartmentId {
        Objects.requireNonNull(value, "Compartment ID cannot be null");
    }

    /**
     * Generates a new compartment ID with a random UUID.
     *
     * @return a new compartment ID
     */
    public static CompartmentId generate() {
        return new CompartmentId(UUID.randomUUID());
    }

    /**
     * Creates a compartment ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a compartment ID wrapping the given UUID
     */
    public static CompartmentId of(UUID value) {
        return new CompartmentId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
