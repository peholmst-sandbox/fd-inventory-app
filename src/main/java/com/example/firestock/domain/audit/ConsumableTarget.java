package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;

import java.util.Objects;

/**
 * Target representing a consumable stock entry being audited.
 *
 * <p>Consumable stock entries track quantities of consumable items at a location.
 * During an audit, the expected quantity is compared against the actual quantity
 * found, and expiry dates are verified.
 *
 * @param consumableStockId the unique identifier of the consumable stock entry being audited
 */
public record ConsumableTarget(ConsumableStockId consumableStockId) implements AuditedItemTarget {

    public ConsumableTarget {
        Objects.requireNonNull(consumableStockId, "Consumable stock ID cannot be null");
    }

    /**
     * Creates a consumable target for the given consumable stock ID.
     *
     * @param consumableStockId the consumable stock ID
     * @return a new consumable target
     */
    public static ConsumableTarget of(ConsumableStockId consumableStockId) {
        return new ConsumableTarget(consumableStockId);
    }
}
