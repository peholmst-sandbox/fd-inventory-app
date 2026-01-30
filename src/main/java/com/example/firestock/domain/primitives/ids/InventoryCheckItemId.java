package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an inventory check item.
 *
 * <p>An inventory check item records the verification result for a single equipment
 * item or consumable stock entry during an inventory check. It captures what was
 * checked, the outcome (present, damaged, missing, etc.), and any observations.
 *
 * @param value the UUID value, must not be null
 */
public record InventoryCheckItemId(UUID value) {
    public InventoryCheckItemId {
        Objects.requireNonNull(value, "Inventory check item ID cannot be null");
    }

    /**
     * Generates a new inventory check item ID with a random UUID.
     *
     * @return a new inventory check item ID
     */
    public static InventoryCheckItemId generate() {
        return new InventoryCheckItemId(UUID.randomUUID());
    }

    /**
     * Creates an inventory check item ID from an existing UUID.
     *
     * @param value the UUID value
     * @return an inventory check item ID wrapping the given UUID
     */
    public static InventoryCheckItemId of(UUID value) {
        return new InventoryCheckItemId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
