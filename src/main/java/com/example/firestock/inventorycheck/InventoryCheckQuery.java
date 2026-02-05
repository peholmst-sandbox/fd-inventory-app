package com.example.firestock.inventorycheck;

import com.example.firestock.domain.inventorycheck.ConsumableCheckTarget;
import com.example.firestock.domain.inventorycheck.CheckedItemTarget;
import com.example.firestock.domain.inventorycheck.EquipmentCheckTarget;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.CheckStatus;
import com.example.firestock.jooq.tables.records.InventoryCheckRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.firestock.jooq.Tables.APPARATUS;
import static com.example.firestock.jooq.Tables.INVENTORY_CHECK;
import static com.example.firestock.jooq.Tables.INVENTORY_CHECK_ITEM;

/**
 * Query class for inventory check read operations.
 */
@Component
class InventoryCheckQuery {

    private final DSLContext create;

    InventoryCheckQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds an active (IN_PROGRESS) inventory check for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the active check record, or empty if none exists
     */
    Optional<InventoryCheckRecord> findActiveByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
            .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
            .fetchOptional();
    }

    /**
     * Finds the latest completed check date for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the completion date, or empty if no completed checks exist
     */
    Optional<Instant> findLatestCompletedDate(ApparatusId apparatusId) {
        return create.select(INVENTORY_CHECK.COMPLETED_AT)
            .from(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
            .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.COMPLETED))
            .orderBy(INVENTORY_CHECK.COMPLETED_AT.desc())
            .limit(1)
            .fetchOptional(INVENTORY_CHECK.COMPLETED_AT);
    }

    /**
     * Finds an inventory check by ID.
     *
     * @param id the check ID
     * @return the check summary, or empty if not found
     */
    Optional<InventoryCheckSummary> findById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.ID.eq(id))
            .fetchOptional()
            .map(r -> new InventoryCheckSummary(
                r.getId(),
                r.getApparatusId(),
                r.getStatus(),
                r.getStartedAt(),
                r.getCompletedAt(),
                r.getTotalItems(),
                r.getVerifiedCount(),
                r.getIssuesFoundCount()
            ));
    }

    /**
     * Finds the inventory check record by ID for updates.
     *
     * @param id the check ID
     * @return the record, or empty if not found
     */
    Optional<InventoryCheckRecord> findRecordById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.ID.eq(id))
            .fetchOptional();
    }

    /**
     * Finds any active check for a station.
     *
     * @param stationId the station to check
     * @return the active check info, or empty if no active checks
     */
    Optional<ActiveCheckInfo> findActiveCheckForStation(StationId stationId) {
        return create.select(
                INVENTORY_CHECK.ID,
                INVENTORY_CHECK.APPARATUS_ID,
                APPARATUS.UNIT_NUMBER,
                INVENTORY_CHECK.STARTED_AT,
                INVENTORY_CHECK.TOTAL_ITEMS,
                INVENTORY_CHECK.VERIFIED_COUNT)
            .from(INVENTORY_CHECK)
            .join(APPARATUS).on(INVENTORY_CHECK.APPARATUS_ID.eq(APPARATUS.ID))
            .where(INVENTORY_CHECK.STATION_ID.eq(stationId))
            .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
            .orderBy(INVENTORY_CHECK.STARTED_AT.desc())
            .limit(1)
            .fetchOptional(r -> new ActiveCheckInfo(
                r.value1(),
                r.value2(),
                r.value3().value(),
                r.value4(),
                r.value5(),
                r.value6()
            ));
    }

    /**
     * Gets verification records for all items in a compartment for a specific check.
     * Returns items with their verification status and verifier UserId.
     *
     * @param checkId the inventory check ID
     * @param compartmentId the compartment to get items for
     * @return list of verification records
     */
    List<ItemVerificationRecord> findVerifiedItemsInCompartment(
            InventoryCheckId checkId, CompartmentId compartmentId) {
        List<ItemVerificationRecord> results = new ArrayList<>();

        create.select(
                INVENTORY_CHECK_ITEM.EQUIPMENT_ITEM_ID,
                INVENTORY_CHECK_ITEM.CONSUMABLE_STOCK_ID,
                INVENTORY_CHECK_ITEM.VERIFICATION_STATUS,
                INVENTORY_CHECK_ITEM.VERIFIED_AT,
                INVENTORY_CHECK_ITEM.VERIFIED_BY_ID)
            .from(INVENTORY_CHECK_ITEM)
            .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
            .and(INVENTORY_CHECK_ITEM.COMPARTMENT_ID.eq(compartmentId))
            .forEach(r -> {
                CheckedItemTarget target;
                if (r.value1() != null) {
                    target = new EquipmentCheckTarget(r.value1());
                } else {
                    target = new ConsumableCheckTarget(r.value2());
                }
                // Convert UUID to UserId
                UserId verifiedBy = r.value5() != null ? new UserId(r.value5()) : null;
                results.add(new ItemVerificationRecord(
                    target,
                    r.value3(),
                    r.value4(),
                    verifiedBy
                ));
            });

        return results;
    }

    /**
     * Counts verified items per compartment for a check.
     *
     * @param checkId the inventory check ID
     * @return map of CompartmentId to verified item count
     */
    Map<CompartmentId, Integer> countVerifiedItemsByCompartment(InventoryCheckId checkId) {
        Map<CompartmentId, Integer> result = new HashMap<>();

        create.select(
                INVENTORY_CHECK_ITEM.COMPARTMENT_ID,
                DSL.count())
            .from(INVENTORY_CHECK_ITEM)
            .where(INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId))
            .groupBy(INVENTORY_CHECK_ITEM.COMPARTMENT_ID)
            .forEach(r -> result.put(r.value1(), r.value2()));

        return result;
    }
}
