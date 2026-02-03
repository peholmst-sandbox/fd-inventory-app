package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.ExpiryStatus;
import com.example.firestock.jooq.enums.ItemCondition;
import com.example.firestock.jooq.enums.TestResult;
import com.example.firestock.jooq.tables.records.FormalAuditItemRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.FORMAL_AUDIT_ITEM;

/**
 * DAO class for formal audit item write operations.
 */
@Component
class FormalAuditItemDao {

    private final DSLContext create;

    FormalAuditItemDao(DSLContext create) {
        this.create = create;
    }

    /**
     * Creates a new formal audit item record.
     *
     * @param auditId the formal audit this item belongs to
     * @param compartmentId the compartment where the item is located
     * @param equipmentItemId the equipment item, or null for consumables
     * @param consumableStockId the consumable stock, or null for equipment
     * @param manifestEntryId the manifest entry reference, if any
     * @param status the audit result status
     * @param condition the item condition
     * @param testResult the test result
     * @param expiryStatus the expiry status
     * @param conditionNotes notes about condition
     * @param testNotes notes about testing
     * @param quantityFound quantity found for consumables
     * @param quantityExpected expected quantity for consumables
     * @param isUnexpected true if this item was not on the manifest
     * @param issueId the linked issue ID, if an issue was created
     * @return the ID of the created audit item
     */
    FormalAuditItemId insert(
            FormalAuditId auditId,
            CompartmentId compartmentId,
            EquipmentItemId equipmentItemId,
            ConsumableStockId consumableStockId,
            ManifestEntryId manifestEntryId,
            AuditItemStatus status,
            ItemCondition condition,
            TestResult testResult,
            ExpiryStatus expiryStatus,
            String conditionNotes,
            String testNotes,
            Quantity quantityFound,
            Quantity quantityExpected,
            boolean isUnexpected,
            IssueId issueId) {

        FormalAuditItemRecord record = create.newRecord(FORMAL_AUDIT_ITEM);
        record.setFormalAuditId(auditId);
        record.setCompartmentId(compartmentId);
        record.setEquipmentItemId(equipmentItemId);
        record.setConsumableStockId(consumableStockId);
        record.setManifestEntryId(manifestEntryId);
        record.setAuditItemStatus(status);
        record.setItemCondition(condition);
        record.setTestResult(testResult);
        record.setExpiryStatus(expiryStatus);
        record.setConditionNotes(conditionNotes);
        record.setTestNotes(testNotes);
        record.setQuantityFound(quantityFound);
        record.setQuantityExpected(quantityExpected);
        record.setIsUnexpected(isUnexpected);
        record.setIssueId(issueId);
        record.store();
        return record.getId();
    }

    /**
     * Checks if an item has already been audited in this formal audit.
     *
     * @param auditId the formal audit ID
     * @param equipmentItemId the equipment item ID, or null
     * @param consumableStockId the consumable stock ID, or null
     * @return true if the item has already been audited
     */
    boolean existsForItem(FormalAuditId auditId, EquipmentItemId equipmentItemId,
                          ConsumableStockId consumableStockId) {
        var condition = FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId);

        if (equipmentItemId != null) {
            condition = condition.and(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId));
        } else if (consumableStockId != null) {
            condition = condition.and(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId));
        } else {
            return false;
        }

        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM).where(condition)
        );
    }

    /**
     * Counts the number of items audited in a formal audit.
     *
     * @param auditId the formal audit ID
     * @return the count of audited items
     */
    int countByAuditId(FormalAuditId auditId) {
        return create.fetchCount(FORMAL_AUDIT_ITEM,
                FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId));
    }
}
