package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.ExpiryStatus;
import com.example.firestock.jooq.enums.ItemCondition;
import com.example.firestock.jooq.enums.TestResult;

/**
 * Request object for auditing an individual item during a formal audit.
 *
 * @param auditId the formal audit this item belongs to
 * @param equipmentItemId the equipment item being audited, or null for consumables
 * @param consumableStockId the consumable stock being audited, or null for equipment
 * @param compartmentId the compartment where the item is located
 * @param manifestEntryId the manifest entry reference, if applicable
 * @param status the audit result status
 * @param condition the physical condition of the item
 * @param testResult the result of functional testing, if applicable
 * @param expiryStatus the expiry status of the item
 * @param conditionNotes notes about the item's condition
 * @param testNotes notes about test results
 * @param quantityFound the quantity found, for consumables
 * @param quantityExpected the expected quantity, for consumables
 * @param isUnexpected true if this item was not on the manifest
 */
public record AuditItemRequest(
        FormalAuditId auditId,
        EquipmentItemId equipmentItemId,
        ConsumableStockId consumableStockId,
        CompartmentId compartmentId,
        ManifestEntryId manifestEntryId,
        AuditItemStatus status,
        ItemCondition condition,
        TestResult testResult,
        ExpiryStatus expiryStatus,
        String conditionNotes,
        String testNotes,
        Quantity quantityFound,
        Quantity quantityExpected,
        boolean isUnexpected
) {
    /**
     * Returns true if this is an audit for a consumable stock item.
     */
    public boolean isConsumable() {
        return consumableStockId != null;
    }

    /**
     * Returns true if this audit result requires creating an issue.
     * Issues are created for MISSING, DAMAGED, FAILED_INSPECTION, and EXPIRED statuses.
     */
    public boolean requiresIssue() {
        return status == AuditItemStatus.MISSING
                || status == AuditItemStatus.DAMAGED
                || status == AuditItemStatus.FAILED_INSPECTION
                || status == AuditItemStatus.EXPIRED;
    }
}
