package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.audit.AuditItemStatus;
import com.example.firestock.domain.audit.AuditedItemTarget;
import com.example.firestock.domain.audit.ConsumableTarget;
import com.example.firestock.domain.audit.EquipmentTarget;
import com.example.firestock.domain.audit.ExpiryStatus;
import com.example.firestock.domain.audit.FormalAuditItem;
import com.example.firestock.domain.audit.ItemCondition;
import com.example.firestock.domain.audit.QuantityComparison;
import com.example.firestock.domain.audit.TestResult;
import com.example.firestock.jooq.tables.records.FormalAuditItemRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between {@link FormalAuditItem} domain objects and
 * {@link FormalAuditItemRecord} jOOQ records.
 *
 * <p>Handles the polymorphic target mapping (XOR between equipment and consumable).
 */
@Component
public class FormalAuditItemMapper {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

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
                toDomainStatus(record.getAuditItemStatus()),
                toDomainCondition(record.getItemCondition()),
                toDomainTestResult(record.getTestResult()),
                toDomainExpiryStatus(record.getExpiryStatus()),
                quantityComparison,
                record.getConditionNotes(),
                record.getTestNotes(),
                toInstant(record.getAuditedAt())
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
        record.setAuditItemStatus(toJooqStatus(item.status()));
        record.setItemCondition(toJooqCondition(item.condition()));
        record.setTestResult(toJooqTestResult(item.testResult()));
        record.setExpiryStatus(toJooqExpiryStatus(item.expiryStatus()));
        record.setConditionNotes(item.conditionNotes());
        record.setTestNotes(item.testNotes());
        record.setAuditedAt(toLocalDateTime(item.auditedAt()));

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

    // ========================================================================
    // Status Enum Conversions
    // ========================================================================

    public com.example.firestock.jooq.enums.AuditItemStatus toJooqStatus(AuditItemStatus status) {
        return com.example.firestock.jooq.enums.AuditItemStatus.valueOf(status.name());
    }

    public AuditItemStatus toDomainStatus(com.example.firestock.jooq.enums.AuditItemStatus status) {
        return AuditItemStatus.valueOf(status.name());
    }

    // ========================================================================
    // Condition Enum Conversions
    // ========================================================================

    public com.example.firestock.jooq.enums.ItemCondition toJooqCondition(ItemCondition condition) {
        return condition == null ? null : com.example.firestock.jooq.enums.ItemCondition.valueOf(condition.name());
    }

    public ItemCondition toDomainCondition(com.example.firestock.jooq.enums.ItemCondition condition) {
        return condition == null ? null : ItemCondition.valueOf(condition.name());
    }

    // ========================================================================
    // TestResult Enum Conversions
    // ========================================================================

    public com.example.firestock.jooq.enums.TestResult toJooqTestResult(TestResult testResult) {
        return testResult == null ? null : com.example.firestock.jooq.enums.TestResult.valueOf(testResult.name());
    }

    public TestResult toDomainTestResult(com.example.firestock.jooq.enums.TestResult testResult) {
        return testResult == null ? null : TestResult.valueOf(testResult.name());
    }

    // ========================================================================
    // ExpiryStatus Enum Conversions
    // ========================================================================

    public com.example.firestock.jooq.enums.ExpiryStatus toJooqExpiryStatus(ExpiryStatus expiryStatus) {
        return expiryStatus == null ? null : com.example.firestock.jooq.enums.ExpiryStatus.valueOf(expiryStatus.name());
    }

    public ExpiryStatus toDomainExpiryStatus(com.example.firestock.jooq.enums.ExpiryStatus expiryStatus) {
        return expiryStatus == null ? null : ExpiryStatus.valueOf(expiryStatus.name());
    }

    // ========================================================================
    // Timestamp Conversions
    // ========================================================================

    public Instant toInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(SYSTEM_ZONE).toInstant();
    }

    public LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, SYSTEM_ZONE);
    }
}
