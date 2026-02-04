package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.jooq.enums.CheckStatus;
import com.example.firestock.jooq.tables.records.InventoryCheckRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK;

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
}
