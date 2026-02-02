package com.example.firestock.inventorycheck.query;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.InventoryCheckItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.jooq.enums.VerificationStatus;
import com.example.firestock.jooq.tables.records.InventoryCheckItemRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK_ITEM;

/**
 * Query class for inventory check item data access operations.
 */
@Component
public class InventoryCheckItemQuery {

    private final DSLContext create;

    public InventoryCheckItemQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Records a verification of an item during an inventory check.
     *
     * @param checkId the inventory check
     * @param compartmentId the compartment where the item is located
     * @param equipmentItemId the equipment item, or null for consumables
     * @param consumableStockId the consumable stock, or null for equipment
     * @param manifestEntryId the manifest entry, or null
     * @param status the verification status
     * @param notes any condition notes
     * @param quantityFound the quantity found for consumables
     * @param quantityExpected the expected quantity for consumables
     * @param issueId the created issue ID, if any
     * @return the ID of the created verification record
     */
    public InventoryCheckItemId insert(
            InventoryCheckId checkId,
            CompartmentId compartmentId,
            EquipmentItemId equipmentItemId,
            ConsumableStockId consumableStockId,
            ManifestEntryId manifestEntryId,
            VerificationStatus status,
            String notes,
            Quantity quantityFound,
            Quantity quantityExpected,
            IssueId issueId) {

        InventoryCheckItemRecord record = create.newRecord(INVENTORY_CHECK_ITEM);
        record.setInventoryCheckId(checkId);
        record.setCompartmentId(compartmentId);
        record.setEquipmentItemId(equipmentItemId);
        record.setConsumableStockId(consumableStockId);
        record.setManifestEntryId(manifestEntryId);
        record.setVerificationStatus(status);
        record.setConditionNotes(notes);
        record.setQuantityFound(quantityFound);
        record.setQuantityExpected(quantityExpected);
        record.setIssueId(issueId);
        record.store();
        return record.getId();
    }

    /**
     * Counts the number of verified items for a check.
     *
     * @param checkId the inventory check ID
     * @return the count of verified items
     */
    public int countByCheckId(InventoryCheckId checkId) {
        return create.fetchCount(INVENTORY_CHECK_ITEM,
            INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId));
    }

    /**
     * Checks if an item has already been verified in this check.
     *
     * @param checkId the inventory check ID
     * @param equipmentItemId the equipment item ID, or null
     * @param consumableStockId the consumable stock ID, or null
     * @return true if already verified
     */
    public boolean existsForItem(InventoryCheckId checkId, EquipmentItemId equipmentItemId,
                                  ConsumableStockId consumableStockId) {
        var condition = INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId);

        if (equipmentItemId != null) {
            condition = condition.and(INVENTORY_CHECK_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId));
        } else if (consumableStockId != null) {
            condition = condition.and(INVENTORY_CHECK_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId));
        } else {
            return false;
        }

        return create.fetchExists(INVENTORY_CHECK_ITEM, condition);
    }
}
