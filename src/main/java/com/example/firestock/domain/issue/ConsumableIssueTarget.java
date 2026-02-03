package com.example.firestock.domain.issue;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;

import java.util.Objects;

/**
 * Issue target representing a consumable stock entry.
 *
 * @param consumableStockId the consumable stock ID
 */
public record ConsumableIssueTarget(ConsumableStockId consumableStockId) implements IssueTarget {

    public ConsumableIssueTarget {
        Objects.requireNonNull(consumableStockId, "Consumable stock ID cannot be null");
    }
}
