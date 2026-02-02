package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.jooq.enums.CheckStatus;

import java.time.LocalDateTime;

/**
 * Summary information about an inventory check.
 *
 * @param id the inventory check ID
 * @param apparatusId the apparatus being checked
 * @param status the current status of the check
 * @param startedAt when the check was started
 * @param completedAt when the check was completed, or null if not yet completed
 * @param totalItems the total number of items to verify
 * @param verifiedCount the number of items that have been verified
 * @param issuesFoundCount the number of issues found during the check
 */
public record InventoryCheckSummary(
    InventoryCheckId id,
    ApparatusId apparatusId,
    CheckStatus status,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    int totalItems,
    int verifiedCount,
    int issuesFoundCount
) {
    /**
     * Returns the progress as a percentage (0-100).
     */
    public int progressPercentage() {
        if (totalItems == 0) return 100;
        return (verifiedCount * 100) / totalItems;
    }

    /**
     * Returns true if all items have been verified.
     */
    public boolean isComplete() {
        return verifiedCount >= totalItems;
    }
}
