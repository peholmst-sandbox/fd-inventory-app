package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;

import java.time.Instant;

/**
 * Information about an active inventory check for display purposes.
 * Used for the "resume" banner when a check is already in progress.
 *
 * @param checkId the inventory check ID
 * @param apparatusId the apparatus being checked
 * @param apparatusUnitNumber the unit number of the apparatus (e.g., "Engine 5")
 * @param startedAt when the check was started
 * @param totalItems the total number of items to verify
 * @param verifiedCount the number of items that have been verified
 */
public record ActiveCheckInfo(
    InventoryCheckId checkId,
    ApparatusId apparatusId,
    String apparatusUnitNumber,
    Instant startedAt,
    int totalItems,
    int verifiedCount
) {
    /**
     * Returns the progress as a percentage (0-100).
     */
    public int progressPercentage() {
        if (totalItems == 0) return 100;
        return (verifiedCount * 100) / totalItems;
    }
}
