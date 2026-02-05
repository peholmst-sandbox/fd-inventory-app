package com.example.firestock.inventorycheck;

import com.example.firestock.jooq.enums.VerificationStatus;

import java.time.Instant;

/**
 * A checkable item with its verification status in the current inventory check.
 * Combines the item information with how it was verified (if at all).
 *
 * @param item the checkable item (equipment or consumable)
 * @param verificationStatus the verification status, or null if not yet verified
 * @param verifiedAt when the item was verified, or null if not yet verified
 * @param verifiedByName display name of the user who verified, looked up from the user table,
 *                       or null if not yet verified
 */
public record CheckableItemWithStatus(
    CheckableItem item,
    VerificationStatus verificationStatus,
    Instant verifiedAt,
    String verifiedByName
) {
    /**
     * Returns true if this item has been verified.
     */
    public boolean isVerified() {
        return verificationStatus != null;
    }

    /**
     * Returns true if this item has an issue (missing, damaged, or expired).
     */
    public boolean hasIssue() {
        return verificationStatus == VerificationStatus.MISSING
            || verificationStatus == VerificationStatus.PRESENT_DAMAGED
            || verificationStatus == VerificationStatus.EXPIRED;
    }
}
