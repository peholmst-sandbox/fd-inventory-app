package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an individual equipment item.
 *
 * <p>An equipment item is an individual, trackable piece of equipment identified by
 * a unique serial number. Equipment items are instances of serialised equipment types
 * and are assigned to specific apparatus and compartments.
 *
 * @param value the UUID value, must not be null
 */
public record EquipmentItemId(UUID value) {
    public EquipmentItemId {
        Objects.requireNonNull(value, "Equipment item ID cannot be null");
    }

    /**
     * Generates a new equipment item ID with a random UUID.
     *
     * @return a new equipment item ID
     */
    public static EquipmentItemId generate() {
        return new EquipmentItemId(UUID.randomUUID());
    }

    /**
     * Creates an equipment item ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an equipment item ID wrapping the given UUID
     */
    public static EquipmentItemId of(UUID value) {
        return new EquipmentItemId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
