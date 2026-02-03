package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;

import java.util.Objects;

/**
 * Target representing a consumable stock entry being verified during an inventory check.
 *
 * <p>Consumable stock entries track quantities of consumable items at a location.
 * During an inventory check, the expected quantity is compared against the actual quantity found.
 *
 * @param consumableStockId the unique identifier of the consumable stock entry being verified
 */
public record ConsumableCheckTarget(ConsumableStockId consumableStockId) implements CheckedItemTarget {

    public ConsumableCheckTarget {
        Objects.requireNonNull(consumableStockId, "Consumable stock ID cannot be null");
    }

    /**
     * Creates a consumable target for the given consumable stock ID.
     *
     * @param consumableStockId the consumable stock ID
     * @return a new consumable target
     */
    public static ConsumableCheckTarget of(ConsumableStockId consumableStockId) {
        return new ConsumableCheckTarget(consumableStockId);
    }
}
