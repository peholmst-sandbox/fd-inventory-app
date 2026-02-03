package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.ItemCondition;
import com.example.firestock.jooq.enums.TestResult;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an item that can be audited during a formal audit.
 * Includes both equipment items and consumable stock.
 *
 * @param equipmentItemId the equipment item ID, or null for consumables
 * @param consumableStockId the consumable stock ID, or null for equipment
 * @param manifestEntryId the manifest entry this item fulfills, if any
 * @param name the display name of the item
 * @param typeName the equipment type name
 * @param serialNumber the serial number, for serialized equipment
 * @param currentQuantity the current quantity, for consumables
 * @param requiredQuantity the required quantity from the manifest
 * @param expiryDate the expiry date, if applicable
 * @param requiresTesting whether this item type requires functional testing
 * @param lastTestDate when the item was last tested
 * @param nextTestDueDate when the next test is due
 * @param isConsumable whether this is a consumable item
 * @param isCritical whether this is a critical item per the manifest
 * @param auditStatus the current audit status, or null if not yet audited
 * @param auditedCondition the condition recorded during audit, or null
 * @param auditedTestResult the test result recorded during audit, or null
 */
public record AuditableItem(
        EquipmentItemId equipmentItemId,
        ConsumableStockId consumableStockId,
        ManifestEntryId manifestEntryId,
        String name,
        String typeName,
        SerialNumber serialNumber,
        Quantity currentQuantity,
        BigDecimal requiredQuantity,
        LocalDate expiryDate,
        boolean requiresTesting,
        LocalDate lastTestDate,
        LocalDate nextTestDueDate,
        boolean isConsumable,
        boolean isCritical,
        AuditItemStatus auditStatus,
        ItemCondition auditedCondition,
        TestResult auditedTestResult
) {
    /**
     * Returns true if this item has been audited in the current audit.
     */
    public boolean isAudited() {
        return auditStatus != null && auditStatus != AuditItemStatus.NOT_AUDITED;
    }

    /**
     * Returns true if the item is expired or expiring soon.
     */
    public boolean hasExpiryWarning() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now().plusDays(30));
    }

    /**
     * Returns true if testing is due (past the next test due date).
     */
    public boolean isTestingOverdue() {
        if (nextTestDueDate == null) {
            return false;
        }
        return nextTestDueDate.isBefore(LocalDate.now());
    }

    /**
     * Returns a unique identifier for this item (for UI tracking).
     */
    public String uniqueId() {
        if (equipmentItemId != null) {
            return "eq-" + equipmentItemId.toString();
        }
        return "cs-" + consumableStockId.toString();
    }
}
