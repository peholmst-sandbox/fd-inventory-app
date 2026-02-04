package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.audit.AbandonedAudit;
import com.example.firestock.domain.audit.AuditProgress;
import com.example.firestock.domain.audit.CompletedAudit;
import com.example.firestock.domain.audit.FormalAudit;
import com.example.firestock.domain.audit.InProgressAudit;
import com.example.firestock.jooq.enums.AuditStatus;
import com.example.firestock.jooq.tables.records.FormalAuditRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link FormalAudit} domain objects and
 * {@link FormalAuditRecord} jOOQ records.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Component
public class FormalAuditMapper {

    /**
     * Converts a jOOQ record to the appropriate domain type based on status.
     *
     * @param record the jOOQ record
     * @return the domain audit in its correct state
     */
    public FormalAudit toDomain(FormalAuditRecord record) {
        if (record == null) {
            return null;
        }

        var status = record.getStatus();
        var progress = extractProgress(record);
        var startedAt = record.getStartedAt();

        return switch (status) {
            case IN_PROGRESS -> new InProgressAudit(
                    record.getId(),
                    record.getApparatusId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    record.getPausedAt(),
                    record.getUpdatedAt() != null ? record.getUpdatedAt() : startedAt,
                    record.getNotes()
            );
            case COMPLETED -> new CompletedAudit(
                    record.getId(),
                    record.getApparatusId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    record.getCompletedAt()
            );
            case ABANDONED -> new AbandonedAudit(
                    record.getId(),
                    record.getApparatusId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    record.getAbandonedAt(),
                    record.getNotes()
            );
        };
    }

    /**
     * Updates a jOOQ record from a domain FormalAudit.
     *
     * @param record the record to update
     * @param audit the domain audit
     */
    public void updateRecord(FormalAuditRecord record, FormalAudit audit) {
        record.setId(audit.id());
        record.setApparatusId(audit.apparatusId());
        record.setPerformedById(audit.auditorId());
        record.setStatus(audit.status());
        record.setStartedAt(audit.startedAt());

        // Set progress fields
        var progress = audit.progress();
        record.setTotalItems(progress.totalItems());
        record.setAuditedCount(progress.auditedCount());
        record.setIssuesFoundCount(progress.issuesFoundCount());
        record.setUnexpectedItemsCount(progress.unexpectedItemsCount());

        // Set state-specific fields
        switch (audit) {
            case InProgressAudit inProgress -> {
                record.setPausedAt(inProgress.pausedAt());
                record.setUpdatedAt(inProgress.lastActivityAt());
                record.setNotes(inProgress.notes());
                record.setCompletedAt(null);
                record.setAbandonedAt(null);
            }
            case CompletedAudit completed -> {
                record.setCompletedAt(completed.completedAt());
                record.setPausedAt(null);
                record.setAbandonedAt(null);
            }
            case AbandonedAudit abandoned -> {
                record.setAbandonedAt(abandoned.abandonedAt());
                record.setNotes(abandoned.reason());
                record.setPausedAt(null);
                record.setCompletedAt(null);
            }
        }
    }

    /**
     * Extracts AuditProgress from a jOOQ record.
     *
     * @param record the jOOQ record
     * @return the audit progress
     */
    private AuditProgress extractProgress(FormalAuditRecord record) {
        return new AuditProgress(
                record.getTotalItems() != null ? record.getTotalItems() : 0,
                record.getAuditedCount() != null ? record.getAuditedCount() : 0,
                record.getIssuesFoundCount() != null ? record.getIssuesFoundCount() : 0,
                record.getUnexpectedItemsCount() != null ? record.getUnexpectedItemsCount() : 0
        );
    }
}
