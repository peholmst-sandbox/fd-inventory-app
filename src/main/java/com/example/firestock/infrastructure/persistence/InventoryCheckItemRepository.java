package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.inventorycheck.InventoryCheckItem;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.InventoryCheckItemId;
import com.example.firestock.jooq.enums.VerificationStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK_ITEM;

/**
 * Repository for {@link InventoryCheckItem} persistence.
 *
 * <p>InventoryCheckItem represents individual item verification records within an inventory check.
 * While conceptually part of the InventoryCheck aggregate, items are managed through
 * a separate repository because:
 * <ul>
 *   <li>Items are added incrementally during a check session</li>
 *   <li>A check may contain many items</li>
 *   <li>Items need to be queried independently for reporting</li>
 * </ul>
 *
 * <p>Handles the polymorphic target (equipment vs. consumable) via XOR on ID fields.
 */
@Repository
public class InventoryCheckItemRepository {

    private final DSLContext create;
    private final InventoryCheckItemMapper mapper;

    public InventoryCheckItemRepository(DSLContext create, InventoryCheckItemMapper mapper) {
        this.create = create;
        this.mapper = mapper;
    }

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Saves an inventory check item (insert or update).
     *
     * @param item the check item to save
     * @return the saved check item
     */
    public InventoryCheckItem save(InventoryCheckItem item) {
        var record = create.newRecord(INVENTORY_CHECK_ITEM);
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
     * Saves multiple check items in a batch.
     *
     * <p>More efficient than saving items individually for bulk operations.
     *
     * @param items the check items to save
     * @return the saved check items
     */
    public List<InventoryCheckItem> saveAll(List<InventoryCheckItem> items) {
        return items.stream()
                .map(this::save)
                .toList();
    }

    /**
     * Finds a check item by its ID.
     *
     * @param id the check item ID
     * @return the check item, or empty if not found
     */
    public Optional<InventoryCheckItem> findById(InventoryCheckItemId id) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if a check item with the given ID exists.
     *
     * @param id the check item ID
     * @return true if the item exists
     */
    public boolean existsById(InventoryCheckItemId id) {
        return create.fetchExists(
                create.selectFrom(INVENTORY_CHECK_ITEM)
                        .where(INVENTORY_CHECK_ITEM.ID.eq(id))
        );
    }

    /**
     * Deletes a check item by its ID.
     *
     * @param id the check item ID to delete
     */
    public void deleteById(InventoryCheckItemId id) {
        create.deleteFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.ID.eq(id))
                .execute();
    }

    // ========================================================================
    // Queries by Check
    // ========================================================================

    /**
     * Finds all items for a specific check.
     *
     * @param checkId the inventory check ID
     * @return list of all check items, ordered by verified time
     */
    public List<InventoryCheckItem> findByCheckId(InventoryCheckId checkId) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .orderBy(INVENTORY_CHECK_ITEM.VERIFIED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds items with issues for a check.
     *
     * <p>Returns items where status indicates an issue (MISSING, PRESENT_DAMAGED, EXPIRED, LOW_QUANTITY).
     *
     * @param checkId the inventory check ID
     * @return list of items with issues
     */
    public List<InventoryCheckItem> findWithIssuesByCheckId(InventoryCheckId checkId) {
        var issueStatuses = List.of(
                VerificationStatus.MISSING,
                VerificationStatus.PRESENT_DAMAGED,
                VerificationStatus.EXPIRED,
                VerificationStatus.LOW_QUANTITY
        );

        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .and(INVENTORY_CHECK_ITEM.VERIFICATION_STATUS.in(issueStatuses))
                .orderBy(INVENTORY_CHECK_ITEM.VERIFIED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds items by status for a check.
     *
     * @param checkId the inventory check ID
     * @param status the status to filter by
     * @return list of items with the specified status
     */
    public List<InventoryCheckItem> findByCheckIdAndStatus(InventoryCheckId checkId, VerificationStatus status) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .and(INVENTORY_CHECK_ITEM.VERIFICATION_STATUS.eq(status))
                .orderBy(INVENTORY_CHECK_ITEM.VERIFIED_AT.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Deletes all items for a check.
     *
     * <p>Typically used when deleting an entire check.
     *
     * @param checkId the inventory check ID
     * @return the number of items deleted
     */
    public int deleteByCheckId(InventoryCheckId checkId) {
        return create.deleteFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .execute();
    }

    // ========================================================================
    // Queries by Target
    // ========================================================================

    /**
     * Finds a check item for a specific equipment item within a check.
     *
     * <p>Used to enforce BR-04: Each item can only be verified once per check.
     *
     * @param checkId the inventory check ID
     * @param equipmentItemId the equipment item ID
     * @return the check item, or empty if not found
     */
    public Optional<InventoryCheckItem> findByCheckIdAndEquipmentItemId(
            InventoryCheckId checkId,
            EquipmentItemId equipmentItemId
    ) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .and(INVENTORY_CHECK_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Finds a check item for a specific consumable stock within a check.
     *
     * @param checkId the inventory check ID
     * @param consumableStockId the consumable stock ID
     * @return the check item, or empty if not found
     */
    public Optional<InventoryCheckItem> findByCheckIdAndConsumableStockId(
            InventoryCheckId checkId,
            ConsumableStockId consumableStockId
    ) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .and(INVENTORY_CHECK_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    /**
     * Checks if an equipment item has been verified in a specific check.
     *
     * <p>Used to enforce BR-04: Each item can only be verified once per check.
     *
     * @param checkId the inventory check ID
     * @param equipmentItemId the equipment item ID
     * @return true if a verification record exists for this equipment item
     */
    public boolean existsByCheckIdAndEquipmentItemId(InventoryCheckId checkId, EquipmentItemId equipmentItemId) {
        return create.fetchExists(
                create.selectFrom(INVENTORY_CHECK_ITEM)
                        .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                        .and(INVENTORY_CHECK_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
        );
    }

    /**
     * Checks if a consumable stock has been verified in a specific check.
     *
     * @param checkId the inventory check ID
     * @param consumableStockId the consumable stock ID
     * @return true if a verification record exists for this consumable stock
     */
    public boolean existsByCheckIdAndConsumableStockId(InventoryCheckId checkId, ConsumableStockId consumableStockId) {
        return create.fetchExists(
                create.selectFrom(INVENTORY_CHECK_ITEM)
                        .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                        .and(INVENTORY_CHECK_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
        );
    }

    /**
     * Finds all verification records for a specific equipment item across all checks.
     *
     * <p>Useful for equipment history and trend analysis.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of check items, ordered by verifiedAt descending
     */
    public List<InventoryCheckItem> findByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .orderBy(INVENTORY_CHECK_ITEM.VERIFIED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    /**
     * Finds all verification records for a specific consumable stock across all checks.
     *
     * @param consumableStockId the consumable stock ID
     * @return list of check items, ordered by verifiedAt descending
     */
    public List<InventoryCheckItem> findByConsumableStockId(ConsumableStockId consumableStockId) {
        return create.selectFrom(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId))
                .orderBy(INVENTORY_CHECK_ITEM.VERIFIED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    // ========================================================================
    // Counting Operations
    // ========================================================================

    /**
     * Counts all items for a check.
     *
     * @param checkId the inventory check ID
     * @return the total count of items
     */
    public long countByCheckId(InventoryCheckId checkId) {
        return create.selectCount()
                .from(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts items with issues for a check.
     *
     * @param checkId the inventory check ID
     * @return the count of items with issues
     */
    public long countWithIssuesByCheckId(InventoryCheckId checkId) {
        var issueStatuses = List.of(
                VerificationStatus.MISSING,
                VerificationStatus.PRESENT_DAMAGED,
                VerificationStatus.EXPIRED,
                VerificationStatus.LOW_QUANTITY
        );

        return create.selectCount()
                .from(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .and(INVENTORY_CHECK_ITEM.VERIFICATION_STATUS.in(issueStatuses))
                .fetchOne(0, Long.class);
    }

    /**
     * Counts items by status for a check.
     *
     * @param checkId the inventory check ID
     * @param status the status to count
     * @return the count of items with the specified status
     */
    public long countByCheckIdAndStatus(InventoryCheckId checkId, VerificationStatus status) {
        return create.selectCount()
                .from(INVENTORY_CHECK_ITEM)
                .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
                .and(INVENTORY_CHECK_ITEM.VERIFICATION_STATUS.eq(status))
                .fetchOne(0, Long.class);
    }
}
