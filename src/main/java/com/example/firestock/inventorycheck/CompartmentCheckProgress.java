package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;

/**
 * Progress information for a single compartment during an inventory check.
 * Shows how many items have been verified and who (if anyone) is currently checking.
 *
 * @param id the compartment ID
 * @param code the compartment code (e.g., "A1")
 * @param name the compartment name (e.g., "Driver Side Front")
 * @param displayOrder the display order for sorting
 * @param totalItems the total number of items in this compartment
 * @param verifiedCount the number of items that have been verified
 * @param isFullyChecked true if all items have been verified
 * @param currentCheckerName display name of the user currently checking this compartment,
 *                           looked up from the user table, or null if no one is checking
 */
public record CompartmentCheckProgress(
    CompartmentId id,
    String code,
    String name,
    int displayOrder,
    int totalItems,
    int verifiedCount,
    boolean isFullyChecked,
    String currentCheckerName
) {
    /**
     * Returns the progress as a percentage (0-100).
     */
    public int progressPercentage() {
        if (totalItems == 0) return 100;
        return (verifiedCount * 100) / totalItems;
    }
}
