package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.audit.AbandonedAudit;
import com.example.firestock.domain.audit.AuditProgress;
import com.example.firestock.domain.audit.AuditStatus;
import com.example.firestock.domain.audit.CompletedAudit;
import com.example.firestock.domain.audit.FormalAudit;
import com.example.firestock.domain.audit.InProgressAudit;
import com.example.firestock.jooq.tables.records.FormalAuditRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper for converting between {@link FormalAudit} domain objects and
 * {@link FormalAuditRecord} jOOQ records.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Component
public class FormalAuditMapper {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

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

        var status = toDomainStatus(record.getStatus());
        var progress = extractProgress(record);
        var startedAt = toInstant(record.getStartedAt());

        return switch (status) {
            case IN_PROGRESS -> new InProgressAudit(
                    record.getId(),
                    record.getApparatusId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    toInstant(record.getPausedAt()),
                    toInstant(record.getUpdatedAt()) != null ? toInstant(record.getUpdatedAt()) : startedAt
            );
            case COMPLETED -> new CompletedAudit(
                    record.getId(),
                    record.getApparatusId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    toInstant(record.getCompletedAt())
            );
            case ABANDONED -> new AbandonedAudit(
                    record.getId(),
                    record.getApparatusId(),
                    record.getPerformedById(),
                    startedAt,
                    progress,
                    toInstant(record.getAbandonedAt()),
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
        record.setStatus(toJooqStatus(audit.status()));
        record.setStartedAt(toLocalDateTime(audit.startedAt()));

        // Set progress fields
        var progress = audit.progress();
        record.setTotalItems(progress.totalItems());
        record.setAuditedCount(progress.auditedCount());
        record.setIssuesFoundCount(progress.issuesFoundCount());
        record.setUnexpectedItemsCount(progress.unexpectedItemsCount());

        // Set state-specific fields
        switch (audit) {
            case InProgressAudit inProgress -> {
                record.setPausedAt(toLocalDateTime(inProgress.pausedAt()));
                record.setUpdatedAt(toLocalDateTime(inProgress.lastActivityAt()));
                record.setCompletedAt(null);
                record.setAbandonedAt(null);
            }
            case CompletedAudit completed -> {
                record.setCompletedAt(toLocalDateTime(completed.completedAt()));
                record.setPausedAt(null);
                record.setAbandonedAt(null);
            }
            case AbandonedAudit abandoned -> {
                record.setAbandonedAt(toLocalDateTime(abandoned.abandonedAt()));
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

    /**
     * Converts a domain AuditStatus to the jOOQ enum.
     *
     * @param status the domain status
     * @return the jOOQ status enum
     */
    public com.example.firestock.jooq.enums.AuditStatus toJooqStatus(AuditStatus status) {
        return com.example.firestock.jooq.enums.AuditStatus.valueOf(status.name());
    }

    /**
     * Converts a jOOQ AuditStatus enum to the domain enum.
     *
     * @param status the jOOQ status enum
     * @return the domain status
     */
    public AuditStatus toDomainStatus(com.example.firestock.jooq.enums.AuditStatus status) {
        return AuditStatus.valueOf(status.name());
    }

    /**
     * Converts a LocalDateTime to an Instant using the system timezone.
     *
     * @param ldt the local date time
     * @return the instant, or null if input is null
     */
    public Instant toInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(SYSTEM_ZONE).toInstant();
    }

    /**
     * Converts an Instant to a LocalDateTime using the system timezone.
     *
     * @param instant the instant
     * @return the local date time, or null if input is null
     */
    public LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, SYSTEM_ZONE);
    }
}
