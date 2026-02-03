package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.CheckStatus;
import com.example.firestock.jooq.tables.records.InventoryCheckRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK;

/**
 * DAO class for inventory check write operations.
 */
@Component
class InventoryCheckDao {

    private final DSLContext create;

    InventoryCheckDao(DSLContext create) {
        this.create = create;
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
    InventoryCheckId insert(ApparatusId apparatusId, StationId stationId,
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
    void updateStatus(InventoryCheckId id, CheckStatus status,
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
    void markCompleted(InventoryCheckId id) {
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
    void markAbandoned(InventoryCheckId id) {
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
    void incrementCounts(InventoryCheckId id, boolean hasIssue) {
        var update = create.update(INVENTORY_CHECK)
            .set(INVENTORY_CHECK.VERIFIED_COUNT, INVENTORY_CHECK.VERIFIED_COUNT.plus(1))
            .set(INVENTORY_CHECK.UPDATED_AT, LocalDateTime.now());

        if (hasIssue) {
            update = update.set(INVENTORY_CHECK.ISSUES_FOUND_COUNT, INVENTORY_CHECK.ISSUES_FOUND_COUNT.plus(1));
        }

        update.where(INVENTORY_CHECK.ID.eq(id)).execute();
    }
}
