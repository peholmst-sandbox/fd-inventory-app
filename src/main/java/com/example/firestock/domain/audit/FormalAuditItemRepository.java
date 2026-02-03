package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link FormalAuditItem} persistence.
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
 * <h3>Target Types</h3>
 * <p>Each item targets either equipment or consumable stock, enforced by the
 * {@link AuditedItemTarget} sealed interface:
 * <ul>
 *   <li>{@link EquipmentTarget} - for serialized equipment items</li>
 *   <li>{@link ConsumableTarget} - for consumable stock entries</li>
 * </ul>
 */
public interface FormalAuditItemRepository {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves a formal audit item (insert or update).
     *
     * @param item the audit item to save
     * @return the saved audit item
     */
    FormalAuditItem save(FormalAuditItem item);

    /**
     * Saves multiple audit items in a batch.
     *
     * <p>More efficient than saving items individually for bulk operations.
     *
     * @param items the audit items to save
     * @return the saved audit items
     */
    List<FormalAuditItem> saveAll(List<FormalAuditItem> items);

    /**
     * Finds an audit item by its ID.
     *
     * @param id the audit item ID
     * @return the audit item, or empty if not found
     */
    Optional<FormalAuditItem> findById(FormalAuditItemId id);

    /**
     * Checks if an audit item with the given ID exists.
     *
     * @param id the audit item ID
     * @return true if the item exists
     */
    boolean existsById(FormalAuditItemId id);

    /**
     * Deletes an audit item by its ID.
     *
     * <p><b>Note:</b> Deleting audit items should be rare. Consider whether
     * updating the status is more appropriate.
     *
     * @param id the audit item ID to delete
     */
    void deleteById(FormalAuditItemId id);

    // ========================================================================
    // Queries by Audit
    // ========================================================================

    /**
     * Finds all items for a specific audit.
     *
     * @param auditId the formal audit ID
     * @return list of all audit items, ordered by creation time
     */
    List<FormalAuditItem> findByAuditId(FormalAuditId auditId);

    /**
     * Finds all audited items (status != NOT_AUDITED) for an audit.
     *
     * @param auditId the formal audit ID
     * @return list of audited items
     */
    List<FormalAuditItem> findAuditedByAuditId(FormalAuditId auditId);

    /**
     * Finds all unaudited items (status == NOT_AUDITED) for an audit.
     *
     * @param auditId the formal audit ID
     * @return list of unaudited items
     */
    List<FormalAuditItem> findUnauditedByAuditId(FormalAuditId auditId);

    /**
     * Finds items with issues for an audit.
     *
     * <p>Returns items where {@link FormalAuditItem#requiresIssue()} returns true.
     *
     * @param auditId the formal audit ID
     * @return list of items with issues (MISSING, DAMAGED, FAILED_INSPECTION, EXPIRED)
     */
    List<FormalAuditItem> findWithIssuesByAuditId(FormalAuditId auditId);

    /**
     * Finds items by status for an audit.
     *
     * @param auditId the formal audit ID
     * @param status the status to filter by
     * @return list of items with the specified status
     */
    List<FormalAuditItem> findByAuditIdAndStatus(FormalAuditId auditId, AuditItemStatus status);

    /**
     * Deletes all items for an audit.
     *
     * <p>Typically used when deleting an entire audit.
     *
     * @param auditId the formal audit ID
     * @return the number of items deleted
     */
    int deleteByAuditId(FormalAuditId auditId);

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
    Optional<FormalAuditItem> findByAuditIdAndEquipmentItemId(
            FormalAuditId auditId,
            EquipmentItemId equipmentItemId
    );

    /**
     * Finds an audit item for a specific consumable stock within an audit.
     *
     * @param auditId the formal audit ID
     * @param consumableStockId the consumable stock ID
     * @return the audit item, or empty if not found
     */
    Optional<FormalAuditItem> findByAuditIdAndConsumableStockId(
            FormalAuditId auditId,
            ConsumableStockId consumableStockId
    );

    /**
     * Checks if an equipment item has been audited in a specific audit.
     *
     * @param auditId the formal audit ID
     * @param equipmentItemId the equipment item ID
     * @return true if an audit record exists for this equipment item
     */
    boolean existsByAuditIdAndEquipmentItemId(FormalAuditId auditId, EquipmentItemId equipmentItemId);

    /**
     * Checks if a consumable stock has been audited in a specific audit.
     *
     * @param auditId the formal audit ID
     * @param consumableStockId the consumable stock ID
     * @return true if an audit record exists for this consumable stock
     */
    boolean existsByAuditIdAndConsumableStockId(FormalAuditId auditId, ConsumableStockId consumableStockId);

    /**
     * Finds all audit records for a specific equipment item across all audits.
     *
     * <p>Useful for equipment history and trend analysis.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of audit items, ordered by auditedAt descending
     */
    List<FormalAuditItem> findByEquipmentItemId(EquipmentItemId equipmentItemId);

    /**
     * Finds all audit records for a specific consumable stock across all audits.
     *
     * @param consumableStockId the consumable stock ID
     * @return list of audit items, ordered by auditedAt descending
     */
    List<FormalAuditItem> findByConsumableStockId(ConsumableStockId consumableStockId);

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all items for an audit.
     *
     * @param auditId the formal audit ID
     * @return the total count of items
     */
    long countByAuditId(FormalAuditId auditId);

    /**
     * Counts audited items (status != NOT_AUDITED) for an audit.
     *
     * <p>Used to track progress and enforce BR-03 (all items must be audited
     * before completion).
     *
     * @param auditId the formal audit ID
     * @return the count of audited items
     */
    long countAuditedByAuditId(FormalAuditId auditId);

    /**
     * Counts items with issues for an audit.
     *
     * @param auditId the formal audit ID
     * @return the count of items with issues
     */
    long countWithIssuesByAuditId(FormalAuditId auditId);

    /**
     * Counts items by status for an audit.
     *
     * @param auditId the formal audit ID
     * @param status the status to count
     * @return the count of items with the specified status
     */
    long countByAuditIdAndStatus(FormalAuditId auditId, AuditItemStatus status);
}
