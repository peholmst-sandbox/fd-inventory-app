package com.example.firestock.inventorycheck;

import com.example.firestock.domain.inventorycheck.CheckedItemTarget;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.VerificationStatus;

import java.time.Instant;

/**
 * Internal record for verification data from the database.
 * Used by queries to return verification information without the full item details.
 * The target uses the existing sealed interface (EquipmentCheckTarget or ConsumableCheckTarget).
 *
 * @param target the checked item target (equipment or consumable)
 * @param status the verification status
 * @param verifiedAt when the item was verified
 * @param verifiedBy the user ID who verified (not display name - resolved separately)
 */
record ItemVerificationRecord(
    CheckedItemTarget target,
    VerificationStatus status,
    Instant verifiedAt,
    UserId verifiedBy
) {}
