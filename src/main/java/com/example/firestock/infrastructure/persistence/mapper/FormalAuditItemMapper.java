package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.audit.AuditedItemTarget;
import com.example.firestock.domain.audit.ConsumableTarget;
import com.example.firestock.domain.audit.EquipmentTarget;
import com.example.firestock.domain.audit.FormalAuditItem;
import com.example.firestock.domain.audit.QuantityComparison;
import com.example.firestock.jooq.tables.records.FormalAuditItemRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link FormalAuditItem} domain objects and
 * {@link FormalAuditItemRecord} jOOQ records.
 *
 * <p>Handles the polymorphic target mapping (XOR between equipment and consumable).
 */
@Component
public class FormalAuditItemMapper {

    /**
     * Converts a jOOQ record to the domain FormalAuditItem.
     *
     * @param record the jOOQ record
     * @return the domain audit item
     */
    public FormalAuditItem toDomain(FormalAuditItemRecord record) {
        if (record == null) {
            return null;
        }

        var target = mapTarget(record);
        var quantityComparison = mapQuantityComparison(record);

        return new FormalAuditItem(
                record.getId(),
                record.getFormalAuditId(),
                target,
                record.getCompartmentId(),
                record.getManifestEntryId(),
                Boolean.TRUE.equals(record.getIsUnexpected()),
                record.getAuditItemStatus(),
                record.getItemCondition(),
                record.getTestResult(),
                record.getExpiryStatus(),
                quantityComparison,
                record.getConditionNotes(),
                record.getTestNotes(),
                record.getAuditedAt()
        );
    }

    /**
     * Updates a jOOQ record from a domain FormalAuditItem.
     *
     * @param record the record to update
     * @param item the domain audit item
     */
    public void updateRecord(FormalAuditItemRecord record, FormalAuditItem item) {
        record.setId(item.id());
        record.setFormalAuditId(item.auditId());
        record.setCompartmentId(item.compartmentId());
        record.setManifestEntryId(item.manifestEntryId());
        record.setIsUnexpected(item.isUnexpected());
        record.setAuditItemStatus(item.status());
        record.setItemCondition(item.condition());
        record.setTestResult(item.testResult());
        record.setExpiryStatus(item.expiryStatus());
        record.setConditionNotes(item.conditionNotes());
        record.setTestNotes(item.testNotes());
        record.setAuditedAt(item.auditedAt());

        // Set target fields (XOR - only one should be set)
        switch (item.target()) {
            case EquipmentTarget equipment -> {
                record.setEquipmentItemId(equipment.equipmentItemId());
                record.setConsumableStockId(null);
            }
            case ConsumableTarget consumable -> {
                record.setConsumableStockId(consumable.consumableStockId());
                record.setEquipmentItemId(null);
            }
        }

        // Set quantity comparison fields for consumables
        if (item.quantityComparison() != null) {
            record.setQuantityExpected(item.quantityComparison().expected());
            record.setQuantityFound(item.quantityComparison().found());
        } else {
            record.setQuantityExpected(null);
            record.setQuantityFound(null);
        }
    }

    /**
     * Maps the polymorphic target from a jOOQ record.
     *
     * <p>XOR constraint: either equipment_item_id or consumable_stock_id is set.
     *
     * @param record the jOOQ record
     * @return the audit item target
     * @throws IllegalStateException if neither or both IDs are set
     */
    private AuditedItemTarget mapTarget(FormalAuditItemRecord record) {
        var equipmentItemId = record.getEquipmentItemId();
        var consumableStockId = record.getConsumableStockId();

        if (equipmentItemId != null && consumableStockId != null) {
            throw new IllegalStateException(
                    "Both equipment_item_id and consumable_stock_id are set for audit item " + record.getId());
        }

        if (equipmentItemId != null) {
            return new EquipmentTarget(equipmentItemId);
        }

        if (consumableStockId != null) {
            return new ConsumableTarget(consumableStockId);
        }

        throw new IllegalStateException(
                "Neither equipment_item_id nor consumable_stock_id is set for audit item " + record.getId());
    }

    /**
     * Maps quantity comparison fields from a jOOQ record.
     *
     * @param record the jOOQ record
     * @return the quantity comparison, or null if not applicable
     */
    private QuantityComparison mapQuantityComparison(FormalAuditItemRecord record) {
        var expected = record.getQuantityExpected();
        var found = record.getQuantityFound();

        if (expected != null && found != null) {
            return new QuantityComparison(expected, found);
        }

        return null;
    }
}
