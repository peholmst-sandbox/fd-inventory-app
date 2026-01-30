package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an equipment type.
 *
 * <p>An equipment type is a template or category that defines a class of equipment.
 * It specifies common attributes, tracking method (serialised or quantity-based),
 * and any special requirements such as testing schedules or expiry tracking.
 *
 * @param value the UUID value, must not be null
 */
public record EquipmentTypeId(UUID value) {
    public EquipmentTypeId {
        Objects.requireNonNull(value, "Equipment type ID cannot be null");
    }

    /**
     * Generates a new equipment type ID with a random UUID.
     *
     * @return a new equipment type ID
     */
    public static EquipmentTypeId generate() {
        return new EquipmentTypeId(UUID.randomUUID());
    }

    /**
     * Creates an equipment type ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an equipment type ID wrapping the given UUID
     */
    public static EquipmentTypeId of(UUID value) {
        return new EquipmentTypeId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
