package com.example.firestock.domain.primitives.ids;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a consumable stock entry.
 *
 * <p>Consumable stock represents a quantity of consumable items at a specific location.
 * Unlike serialised equipment items, consumables are tracked by count or measure rather
 * than individual identity. Stock records track current quantity, lot information, and
 * expiry dates.
 *
 * @param value the UUID value, must not be null
 */
public record ConsumableStockId(UUID value) {
    public ConsumableStockId {
        Objects.requireNonNull(value, "Consumable stock ID cannot be null");
    }

    /**
     * Generates a new consumable stock ID with a random UUID.
     *
     * @return a new consumable stock ID
     */
    public static ConsumableStockId generate() {
        return new ConsumableStockId(UUID.randomUUID());
    }

    /**
     * Creates a consumable stock ID from an existing UUID.
     *
     * @param value the UUID value
     * @return a consumable stock ID wrapping the given UUID
     */
    public static ConsumableStockId of(UUID value) {
        return new ConsumableStockId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
