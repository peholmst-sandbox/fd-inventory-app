package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an inventory check.
 *
 * <p>An inventory check is a systematic verification of equipment and consumables
 * on an apparatus. Typically performed at shift changes, inventory checks ensure
 * all required items are present and in acceptable condition.
 *
 * @param value the UUID value, must not be null
 */
public record InventoryCheckId(UUID value) {
    public InventoryCheckId {
        Objects.requireNonNull(value, "Inventory check ID cannot be null");
    }

    /**
     * Generates a new inventory check ID with a random UUID.
     *
     * @return a new inventory check ID
     */
    public static InventoryCheckId generate() {
        return new InventoryCheckId(UUID.randomUUID());
    }

    /**
     * Creates an inventory check ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an inventory check ID wrapping the given UUID
     */
    public static InventoryCheckId of(UUID value) {
        return new InventoryCheckId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
