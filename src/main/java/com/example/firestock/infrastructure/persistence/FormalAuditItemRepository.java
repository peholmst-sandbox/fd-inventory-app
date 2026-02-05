package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.audit.FormalAuditItem;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;
import com.example.firestock.jooq.enums.AuditItemStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.FORMAL_AUDIT_ITEM;

/**
 * Repository for {@link FormalAuditItem} persistence.
 *
 * <p>FormalAuditItem represents individual item audit records within a formal audit.
 * While conceptually part of the FormalAudit aggregate, items are managed through
 * a separate repository because:
 * <ul>
 *   <li>Items are added incrementally during an audit session</li>
 *   <li>An audit may contain hundreds of items</li>
 *   <li>Items need to be queried independently for reporting</li>
 * </ul>
 *
 * <p>Handles the polymorphic target (equipment vs. consumable) via XOR on ID fields.
 */
@Repository
public class FormalAuditItemRepository {

    private final DSLContext create;
    private final FormalAuditItemMapper mapper;

    public FormalAuditItemRepository(DSLContext create, FormalAuditItemMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a formal audit item (insert or update).
     *
     * @param item the audit item to save
     * @return the saved audit item
     */
    public FormalAuditItem save(FormalAuditItem item) {
        var record = create.newRecord(FORMAL_AUDIT_ITEM);
        mapper.updateRecord(record, item);

        if (existsById(item.id())) {
            record.update();
        } else {
            record.setCreatedAt(Instant.now());
            record.insert();
        }

        return item;
    }

    /**
     * Saves multiple audit items in a batch.
     *
     * <p>More efficient than saving items individually for bulk operations.
     *
     * @param items the audit items to save
     * @return the saved audit items
     */
    public List<FormalAuditItem> saveAll(List<FormalAuditItem> items) {
        return items.stream()
                .map(this::save)
                .toList();
    }

    /**
     * Finds an audit item by its ID.
     *
     * @param id the audit item ID
     * @return the audit item, or empty if not found
     */
    public Optional<FormalAuditItem> findById(FormalAuditItemId id) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if an audit item with the given ID exists.
     *
     * @param id the audit item ID
     * @return true if the item exists
     */
    public boolean existsById(FormalAuditItemId id) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM)
                        .where(FORMAL_AUDIT_ITEM.ID.eq(id))
        );
    }

    /**
     * Deletes an audit item by its ID.
     *
     * <p><b>Note:</b> Deleting audit items should be rare. Consider whether
     * updating the status is more appropriate.
     *
     * @param id the audit item ID to delete
     */
    public void deleteById(FormalAuditItemId id) {
        create.deleteFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.ID.eq(id))
                .execute();
    }

    // ========================================================================
    // Queries by Audit
    // ========================================================================

    /**
     * Finds all items for a specific audit.
     *
     * @param auditId the formal audit ID
     * @return list of all audit items, ordered by creation time
     */
    public List<FormalAuditItem> findByAuditId(FormalAuditId auditId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .orderBy(FORMAL_AUDIT_ITEM.CREATED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all audited items (status != NOT_AUDITED) for an audit.
     *
     * @param auditId the formal audit ID
     * @return list of audited items
     */
    public List<FormalAuditItem> findAuditedByAuditId(FormalAuditId auditId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.ne(AuditItemStatus.NOT_AUDITED))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all unaudited items (status == NOT_AUDITED) for an audit.
     *
     * @param auditId the formal audit ID
     * @return list of unaudited items
     */
    public List<FormalAuditItem> findUnauditedByAuditId(FormalAuditId auditId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.eq(AuditItemStatus.NOT_AUDITED))
                .orderBy(FORMAL_AUDIT_ITEM.CREATED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds items with issues for an audit.
     *
     * <p>Returns items where status indicates an issue (MISSING, DAMAGED, FAILED_INSPECTION, EXPIRED).
     *
     * @param auditId the formal audit ID
     * @return list of items with issues
     */
    public List<FormalAuditItem> findWithIssuesByAuditId(FormalAuditId auditId) {
        // Statuses that require issue creation per BR-05
        var issueStatuses = List.of(
                AuditItemStatus.MISSING,
                AuditItemStatus.DAMAGED,
                AuditItemStatus.FAILED_INSPECTION,
                AuditItemStatus.EXPIRED
        );

        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.in(issueStatuses))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds items by status for an audit.
     *
     * @param auditId the formal audit ID
     * @param status the status to filter by
     * @return list of items with the specified status
     */
    public List<FormalAuditItem> findByAuditIdAndStatus(FormalAuditId auditId, AuditItemStatus status) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.eq(status))
                .orderBy(FORMAL_AUDIT_ITEM.CREATED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Deletes all items for an audit.
     *
     * <p>Typically used when deleting an entire audit.
     *
     * @param auditId the formal audit ID
     * @return the number of items deleted
     */
    public int deleteByAuditId(FormalAuditId auditId) {
        return create.deleteFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .execute();
    }

    // ========================================================================
    // Queries by Target
    // ========================================================================

    /**
     * Finds an audit item for a specific equipment item within an audit.
     *
     * @param auditId the formal audit ID
     * @param equipmentItemId the equipment item ID
     * @return the audit item, or empty if not found
     */
    public Optional<FormalAuditItem> findByAuditIdAndEquipmentItemId(
            FormalAuditId auditId,
            EquipmentItemId equipmentItemId
    ) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Finds an audit item for a specific consumable stock within an audit.
     *
     * @param auditId the formal audit ID
     * @param consumableStockId the consumable stock ID
     * @return the audit item, or empty if not found
     */
    public Optional<FormalAuditItem> findByAuditIdAndConsumableStockId(
            FormalAuditId auditId,
            ConsumableStockId consumableStockId
    ) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if an equipment item has been audited in a specific audit.
     *
     * @param auditId the formal audit ID
     * @param equipmentItemId the equipment item ID
     * @return true if an audit record exists for this equipment item
     */
    public boolean existsByAuditIdAndEquipmentItemId(FormalAuditId auditId, EquipmentItemId equipmentItemId) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM)
                        .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                        .and(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
        );
    }

    /**
     * Checks if a consumable stock has been audited in a specific audit.
     *
     * @param auditId the formal audit ID
     * @param consumableStockId the consumable stock ID
     * @return true if an audit record exists for this consumable stock
     */
    public boolean existsByAuditIdAndConsumableStockId(FormalAuditId auditId, ConsumableStockId consumableStockId) {
        return create.fetchExists(
                create.selectFrom(FORMAL_AUDIT_ITEM)
                        .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                        .and(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
        );
    }

    /**
     * Finds all audit records for a specific equipment item across all audits.
     *
     * <p>Useful for equipment history and trend analysis.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of audit items, ordered by auditedAt descending
     */
    public List<FormalAuditItem> findByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all audit records for a specific consumable stock across all audits.
     *
     * @param consumableStockId the consumable stock ID
     * @return list of audit items, ordered by auditedAt descending
     */
    public List<FormalAuditItem> findByConsumableStockId(ConsumableStockId consumableStockId) {
        return create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
                .orderBy(FORMAL_AUDIT_ITEM.AUDITED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all items for an audit.
     *
     * @param auditId the formal audit ID
     * @return the total count of items
     */
    public long countByAuditId(FormalAuditId auditId) {
        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts audited items (status != NOT_AUDITED) for an audit.
     *
     * <p>Used to track progress and enforce BR-03 (all items must be audited
     * before completion).
     *
     * @param auditId the formal audit ID
     * @return the count of audited items
     */
    public long countAuditedByAuditId(FormalAuditId auditId) {
        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.ne(AuditItemStatus.NOT_AUDITED))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts items with issues for an audit.
     *
     * @param auditId the formal audit ID
     * @return the count of items with issues
     */
    public long countWithIssuesByAuditId(FormalAuditId auditId) {
        var issueStatuses = List.of(
                AuditItemStatus.MISSING,
                AuditItemStatus.DAMAGED,
                AuditItemStatus.FAILED_INSPECTION,
                AuditItemStatus.EXPIRED
        );

        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.in(issueStatuses))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts items by status for an audit.
     *
     * @param auditId the formal audit ID
     * @param status the status to count
     * @return the count of items with the specified status
     */
    public long countByAuditIdAndStatus(FormalAuditId auditId, AuditItemStatus status) {
        return create.selectCount()
                .from(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(auditId))
                .and(FORMAL_AUDIT_ITEM.AUDIT_ITEM_STATUS.eq(status))
                .fetchOne(0, Long.class);
    }
}
