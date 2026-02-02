package com.example.firestock.inventorycheck.query;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.inventorycheck.dto.InventoryCheckSummary;
import com.example.firestock.jooq.enums.CheckStatus;
import com.example.firestock.jooq.tables.records.InventoryCheckRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK;

/**
 * Query class for inventory check data access operations.
 */
@Component
public class InventoryCheckQuery {

    private final DSLContext create;

    public InventoryCheckQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds an active (IN_PROGRESS) inventory check for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the active check record, or empty if none exists
     */
    public Optional<InventoryCheckRecord> findActiveByApparatusId(ApparatusId apparatusId) {
        return create.selectFrom(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.APPARATUS_ID.eq(apparatusId))
            .and(INVENTORY_CHECK.STATUS.eq(CheckStatus.IN_PROGRESS))
            .fetchOptional();
    }

    /**
     * Creates a new inventory check.
     *
     * @param apparatusId the apparatus being checked
     * @param stationId the station where the check is performed
     * @param performedBy the user performing the check
     * @param totalItems the total number of items to be verified
     * @return the ID of the newly created check
     */
    public InventoryCheckId insert(ApparatusId apparatusId, StationId stationId,
                                   UserId performedBy, int totalItems) {
        InventoryCheckRecord record = create.newRecord(INVENTORY_CHECK);
        record.setApparatusId(apparatusId);
        record.setStationId(stationId);
        record.setPerformedById(performedBy);
        record.setStatus(CheckStatus.IN_PROGRESS);
        record.setTotalItems(totalItems);
        record.setVerifiedCount(0);
        record.setIssuesFoundCount(0);
        record.store();
        return record.getId();
    }

    /**
     * Updates the status and counts of an inventory check.
     *
     * @param id the check ID
     * @param status the new status
     * @param verifiedCount the number of verified items
     * @param issuesFoundCount the number of issues found
     */
    public void updateStatus(InventoryCheckId id, CheckStatus status,
                             int verifiedCount, int issuesFoundCount) {
        create.update(INVENTORY_CHECK)
            .set(INVENTORY_CHECK.STATUS, status)
            .set(INVENTORY_CHECK.VERIFIED_COUNT, verifiedCount)
            .set(INVENTORY_CHECK.ISSUES_FOUND_COUNT, issuesFoundCount)
            .set(INVENTORY_CHECK.UPDATED_AT, LocalDateTime.now())
            .where(INVENTORY_CHECK.ID.eq(id))
            .execute();
    }

    /**
     * Marks a check as completed.
     *
     * @param id the check ID
     */
    public void markCompleted(InventoryCheckId id) {
        create.update(INVENTORY_CHECK)
            .set(INVENTORY_CHECK.STATUS, CheckStatus.COMPLETED)
            .set(INVENTORY_CHECK.COMPLETED_AT, LocalDateTime.now())
            .set(INVENTORY_CHECK.UPDATED_AT, LocalDateTime.now())
            .where(INVENTORY_CHECK.ID.eq(id))
            .execute();
    }

    /**
     * Marks a check as abandoned.
     *
     * @param id the check ID
     */
    public void markAbandoned(InventoryCheckId id) {
        create.update(INVENTORY_CHECK)
            .set(INVENTORY_CHECK.STATUS, CheckStatus.ABANDONED)
            .set(INVENTORY_CHECK.ABANDONED_AT, LocalDateTime.now())
            .set(INVENTORY_CHECK.UPDATED_AT, LocalDateTime.now())
            .where(INVENTORY_CHECK.ID.eq(id))
            .execute();
    }

    /**
     * Increments the verified item count for a check.
     *
     * @param id the check ID
     * @param hasIssue true if the verification found an issue
     */
    public void incrementCounts(InventoryCheckId id, boolean hasIssue) {
        var update = create.update(INVENTORY_CHECK)
            .set(INVENTORY_CHECK.VERIFIED_COUNT, INVENTORY_CHECK.VERIFIED_COUNT.plus(1))
            .set(INVENTORY_CHECK.UPDATED_AT, LocalDateTime.now());

        if (hasIssue) {
            update = update.set(INVENTORY_CHECK.ISSUES_FOUND_COUNT, INVENTORY_CHECK.ISSUES_FOUND_COUNT.plus(1));
        }

        update.where(INVENTORY_CHECK.ID.eq(id)).execute();
    }

    /**
     * Finds the latest completed check date for an apparatus.
     *
     * @param apparatusId the apparatus to check
     * @return the completion date, or empty if no completed checks exist
     */
    public Optional<LocalDateTime> findLatestCompletedDate(ApparatusId apparatusId) {
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
    public Optional<InventoryCheckSummary> findById(InventoryCheckId id) {
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
    public Optional<InventoryCheckRecord> findRecordById(InventoryCheckId id) {
        return create.selectFrom(INVENTORY_CHECK)
            .where(INVENTORY_CHECK.ID.eq(id))
            .fetchOptional();
    }
}
