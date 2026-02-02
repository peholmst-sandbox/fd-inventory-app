package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.jooq.enums.VerificationStatus;

/**
 * Request to record the verification of an item during an inventory check.
 *
 * <p>Exactly one of {@code equipmentItemId} or {@code consumableStockId} must be set.
 *
 * @param checkId the inventory check this verification belongs to
 * @param equipmentItemId the equipment item being verified, or null for consumables
 * @param consumableStockId the consumable stock being verified, or null for equipment
 * @param compartmentId the compartment where the item is located
 * @param manifestEntryId optional manifest entry ID if verifying against a manifest
 * @param status the verification status
 * @param conditionNotes optional notes about the item's condition
 * @param quantityFound for consumables, the actual quantity found
 * @param quantityExpected for consumables, the expected quantity
 */
public record ItemVerificationRequest(
    InventoryCheckId checkId,
    EquipmentItemId equipmentItemId,
    ConsumableStockId consumableStockId,
    CompartmentId compartmentId,
    ManifestEntryId manifestEntryId,
    VerificationStatus status,
    String conditionNotes,
    Quantity quantityFound,
    Quantity quantityExpected
) {
    public ItemVerificationRequest {
        if (checkId == null) {
            throw new IllegalArgumentException("checkId is required");
        }
        if (compartmentId == null) {
            throw new IllegalArgumentException("compartmentId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (equipmentItemId == null && consumableStockId == null) {
            throw new IllegalArgumentException("Either equipmentItemId or consumableStockId must be set");
        }
        if (equipmentItemId != null && consumableStockId != null) {
            throw new IllegalArgumentException("Cannot set both equipmentItemId and consumableStockId");
        }
    }

    /**
     * Returns true if this verification is for a consumable item.
     */
    public boolean isConsumable() {
        return consumableStockId != null;
    }

    /**
     * Returns true if this verification indicates a problem that should create an issue.
     */
    public boolean requiresIssue() {
        return status == VerificationStatus.MISSING
            || status == VerificationStatus.PRESENT_DAMAGED
            || status == VerificationStatus.EXPIRED;
    }
}
