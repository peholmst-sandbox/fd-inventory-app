package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.AuditStatus;
import com.example.firestock.jooq.tables.records.FormalAuditRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.example.firestock.jooq.Tables.FORMAL_AUDIT;

/**
 * DAO class for formal audit write operations.
 */
@Component
class FormalAuditDao {

    private final DSLContext create;

    FormalAuditDao(DSLContext create) {
        this.create = create;
    }

    /**
     * Creates a new formal audit.
     *
     * @param apparatusId the apparatus being audited
     * @param stationId the station where the audit is performed
     * @param performedBy the user performing the audit
     * @param totalItems the total number of items to be audited
     * @return the ID of the newly created audit
     */
    FormalAuditId insert(ApparatusId apparatusId, StationId stationId,
                         UserId performedBy, int totalItems) {
        FormalAuditRecord record = create.newRecord(FORMAL_AUDIT);
        record.setApparatusId(apparatusId);
        record.setStationId(stationId);
        record.setPerformedById(performedBy);
        record.setStatus(AuditStatus.IN_PROGRESS);
        record.setTotalItems(totalItems);
        record.setAuditedCount(0);
        record.setIssuesFoundCount(0);
        record.setUnexpectedItemsCount(0);
        record.store();
        return record.getId();
    }

    /**
     * Marks an audit as completed.
     *
     * @param id the audit ID
     */
    void markCompleted(FormalAuditId id) {
        create.update(FORMAL_AUDIT)
                .set(FORMAL_AUDIT.STATUS, AuditStatus.COMPLETED)
                .set(FORMAL_AUDIT.COMPLETED_AT, LocalDateTime.now())
                .set(FORMAL_AUDIT.PAUSED_AT, (LocalDateTime) null)
                .set(FORMAL_AUDIT.UPDATED_AT, LocalDateTime.now())
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }

    /**
     * Marks an audit as abandoned.
     *
     * @param id the audit ID
     */
    void markAbandoned(FormalAuditId id) {
        create.update(FORMAL_AUDIT)
                .set(FORMAL_AUDIT.STATUS, AuditStatus.ABANDONED)
                .set(FORMAL_AUDIT.ABANDONED_AT, LocalDateTime.now())
                .set(FORMAL_AUDIT.PAUSED_AT, (LocalDateTime) null)
                .set(FORMAL_AUDIT.UPDATED_AT, LocalDateTime.now())
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }

    /**
     * Marks an audit as paused (save and exit for later resumption).
     *
     * @param id the audit ID
     */
    void markPaused(FormalAuditId id) {
        create.update(FORMAL_AUDIT)
                .set(FORMAL_AUDIT.PAUSED_AT, LocalDateTime.now())
                .set(FORMAL_AUDIT.UPDATED_AT, LocalDateTime.now())
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }

    /**
     * Resumes a paused audit (clears the paused_at timestamp).
     *
     * @param id the audit ID
     */
    void resume(FormalAuditId id) {
        create.update(FORMAL_AUDIT)
                .set(FORMAL_AUDIT.PAUSED_AT, (LocalDateTime) null)
                .set(FORMAL_AUDIT.UPDATED_AT, LocalDateTime.now())
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }

    /**
     * Increments the audited item count for an audit.
     *
     * @param id the audit ID
     * @param hasIssue true if the item audit found an issue
     * @param isUnexpected true if the item was not on the manifest
     */
    void incrementCounts(FormalAuditId id, boolean hasIssue, boolean isUnexpected) {
        var update = create.update(FORMAL_AUDIT)
                .set(FORMAL_AUDIT.AUDITED_COUNT, FORMAL_AUDIT.AUDITED_COUNT.plus(1))
                .set(FORMAL_AUDIT.UPDATED_AT, LocalDateTime.now());

        if (hasIssue) {
            update = update.set(FORMAL_AUDIT.ISSUES_FOUND_COUNT, FORMAL_AUDIT.ISSUES_FOUND_COUNT.plus(1));
        }

        if (isUnexpected) {
            update = update.set(FORMAL_AUDIT.UNEXPECTED_ITEMS_COUNT, FORMAL_AUDIT.UNEXPECTED_ITEMS_COUNT.plus(1));
        }

        update.where(FORMAL_AUDIT.ID.eq(id)).execute();
    }

    /**
     * Updates the notes for an audit.
     *
     * @param id the audit ID
     * @param notes the notes to save
     */
    void updateNotes(FormalAuditId id, String notes) {
        create.update(FORMAL_AUDIT)
                .set(FORMAL_AUDIT.NOTES, notes)
                .set(FORMAL_AUDIT.UPDATED_AT, LocalDateTime.now())
                .where(FORMAL_AUDIT.ID.eq(id))
                .execute();
    }
}
