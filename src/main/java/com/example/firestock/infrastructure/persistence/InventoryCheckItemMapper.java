package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.inventorycheck.CheckedItemTarget;
import com.example.firestock.domain.inventorycheck.ConsumableCheckTarget;
import com.example.firestock.domain.inventorycheck.EquipmentCheckTarget;
import com.example.firestock.domain.inventorycheck.InventoryCheckItem;
import com.example.firestock.jooq.tables.records.InventoryCheckItemRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link InventoryCheckItem} domain objects and
 * {@link InventoryCheckItemRecord} jOOQ records.
 *
 * <p>Handles the polymorphic target mapping (XOR between equipment and consumable).
 */
@Component
class InventoryCheckItemMapper {

    /**
     * Converts a jOOQ record to the domain InventoryCheckItem.
     *
     * @param record the jOOQ record
     * @return the domain check item
     */
    public InventoryCheckItem toDomain(InventoryCheckItemRecord record) {
        if (record == null) {
            return null;
        }

        var target = mapTarget(record);

        return new InventoryCheckItem(
                record.getId(),
                record.getInventoryCheckId(),
                target,
                record.getCompartmentId(),
                record.getManifestEntryId(),
                record.getVerificationStatus(),
                record.getQuantityFound(),
                record.getQuantityExpected(),
                record.getConditionNotes(),
                record.getVerifiedAt(),
                record.getIssueId()
        );
    }

    /**
     * Updates a jOOQ record from a domain InventoryCheckItem.
     *
     * @param record the record to update
     * @param item the domain check item
     */
    public void updateRecord(InventoryCheckItemRecord record, InventoryCheckItem item) {
        record.setId(item.id());
        record.setInventoryCheckId(item.checkId());
        record.setCompartmentId(item.compartmentId());
        record.setManifestEntryId(item.manifestEntryId());
        record.setVerificationStatus(item.status());
        record.setQuantityFound(item.quantityFound());
        record.setQuantityExpected(item.quantityExpected());
        record.setConditionNotes(item.conditionNotes());
        record.setVerifiedAt(item.verifiedAt());
        record.setIssueId(item.issueId());

        // Set target fields (XOR - only one should be set)
        switch (item.target()) {
            case EquipmentCheckTarget equipment -> {
                record.setEquipmentItemId(equipment.equipmentItemId());
                record.setConsumableStockId(null);
            }
            case ConsumableCheckTarget consumable -> {
                record.setConsumableStockId(consumable.consumableStockId());
                record.setEquipmentItemId(null);
            }
        }
    }

    /**
     * Maps the polymorphic target from a jOOQ record.
     *
     * <p>XOR constraint: either equipment_item_id or consumable_stock_id is set.
     *
     * @param record the jOOQ record
     * @return the check item target
     * @throws IllegalStateException if neither or both IDs are set
     */
    private CheckedItemTarget mapTarget(InventoryCheckItemRecord record) {
        var equipmentItemId = record.getEquipmentItemId();
        var consumableStockId = record.getConsumableStockId();

        if (equipmentItemId != null && consumableStockId != null) {
            throw new IllegalStateException(
                    "Both equipment_item_id and consumable_stock_id are set for check item " + record.getId());
        }

        if (equipmentItemId != null) {
            return new EquipmentCheckTarget(equipmentItemId);
        }

        if (consumableStockId != null) {
            return new ConsumableCheckTarget(consumableStockId);
        }

        throw new IllegalStateException(
                "Neither equipment_item_id nor consumable_stock_id is set for check item " + record.getId());
    }
}
