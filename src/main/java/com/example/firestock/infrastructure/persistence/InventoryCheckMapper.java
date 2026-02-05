package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.inventorycheck.AbandonedCheck;
import com.example.firestock.domain.inventorycheck.CheckProgress;
import com.example.firestock.domain.inventorycheck.CompletedCheck;
import com.example.firestock.domain.inventorycheck.InProgressCheck;
import com.example.firestock.domain.inventorycheck.InventoryCheck;
import com.example.firestock.jooq.tables.records.InventoryCheckRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link InventoryCheck} domain objects and
 * {@link InventoryCheckRecord} jOOQ records.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Component
class InventoryCheckMapper {

    /**
     * Converts a jOOQ record to the appropriate domain type based on status.
     *
     * @param record the jOOQ record
     * @return the domain check in its correct state
     */
    public InventoryCheck toDomain(InventoryCheckRecord record) {
        if (record == null) {
            return null;
        }

        var status = record.getStatus();
        var progress = extractProgress(record);
        var startedAt = record.getStartedAt();

        return switch (status) {
            case IN_PROGRESS -> new InProgressCheck(
                    record.getId(),
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    record.getUpdatedAt() != null ? record.getUpdatedAt() : startedAt,
                    record.getNotes()
            );
            case COMPLETED -> new CompletedCheck(
                    record.getId(),
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    record.getCompletedAt()
            );
            case ABANDONED -> new AbandonedCheck(
                    record.getId(),
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    record.getAbandonedAt(),
                    record.getNotes()
            );
        };
    }

    /**
     * Updates a jOOQ record from a domain InventoryCheck.
     *
     * @param record the record to update
     * @param check the domain check
     */
    public void updateRecord(InventoryCheckRecord record, InventoryCheck check) {
        record.setId(check.id());
        record.setApparatusId(check.apparatusId());
        record.setStationId(check.stationId());
        record.setPerformedById(check.performedById());
        record.setStatus(check.status());
        record.setStartedAt(check.startedAt());

        // Set progress fields
        var progress = check.progress();
        record.setTotalItems(progress.totalItems());
        record.setVerifiedCount(progress.verifiedCount());
        record.setIssuesFoundCount(progress.issuesFoundCount());

        // Set state-specific fields
        switch (check) {
            case InProgressCheck inProgress -> {
                record.setUpdatedAt(inProgress.lastActivityAt());
                record.setNotes(inProgress.notes());
                record.setCompletedAt(null);
                record.setAbandonedAt(null);
            }
            case CompletedCheck completed -> {
                record.setCompletedAt(completed.completedAt());
                record.setAbandonedAt(null);
            }
            case AbandonedCheck abandoned -> {
                record.setAbandonedAt(abandoned.abandonedAt());
                record.setNotes(abandoned.reason());
                record.setCompletedAt(null);
            }
        }
    }

    /**
     * Extracts CheckProgress from a jOOQ record.
     *
     * @param record the jOOQ record
     * @return the check progress
     */
    private CheckProgress extractProgress(InventoryCheckRecord record) {
        return new CheckProgress(
                record.getTotalItems() != null ? record.getTotalItems() : 0,
                record.getVerifiedCount() != null ? record.getVerifiedCount() : 0,
                record.getIssuesFoundCount() != null ? record.getIssuesFoundCount() : 0
        );
    }
}
