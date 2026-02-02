package com.example.firestock.inventorycheck.dao;

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
 * DAO class for inventory check item write operations.
 */
@Component
public class InventoryCheckItemDao {

    private final DSLContext create;

    public InventoryCheckItemDao(DSLContext create) {
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
}
