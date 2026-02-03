package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.jooq.enums.AuditStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Summary information about a formal audit.
 *
 * @param id the audit ID
 * @param apparatusId the apparatus being audited
 * @param status the current audit status
 * @param startedAt when the audit was started
 * @param completedAt when the audit was completed, or null if not completed
 * @param pausedAt when the audit was last paused, or null if not paused
 * @param totalItems the total number of items to audit
 * @param auditedCount the number of items audited so far
 * @param issuesFoundCount the number of issues found
 * @param unexpectedItemsCount the number of unexpected items found
 */
public record AuditSummary(
        FormalAuditId id,
        ApparatusId apparatusId,
        AuditStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime pausedAt,
        int totalItems,
        int auditedCount,
        int issuesFoundCount,
        int unexpectedItemsCount
) {
    /**
     * Returns the progress percentage (0-100).
     */
    public int progressPercentage() {
        if (totalItems == 0) {
            return 0;
        }
        return Math.min(100, (auditedCount * 100) / totalItems);
    }

    /**
     * Returns true if all items have been audited.
     */
    public boolean isComplete() {
        return auditedCount >= totalItems;
    }

    /**
     * Returns true if the audit is stale (started more than 7 days ago and not completed).
     * Per BR-04: Audits more than 7 days old should be flagged.
     */
    public boolean isStale() {
        if (status == AuditStatus.COMPLETED || status == AuditStatus.ABANDONED) {
            return false;
        }
        return startedAt.until(LocalDateTime.now(), ChronoUnit.DAYS) > 7;
    }

    /**
     * Returns true if the audit is currently paused (saved for later resumption).
     */
    public boolean isPaused() {
        return status == AuditStatus.IN_PROGRESS && pausedAt != null;
    }

    /**
     * Returns the number of days since the audit was started.
     */
    public long daysInProgress() {
        return startedAt.until(LocalDateTime.now(), ChronoUnit.DAYS);
    }
}
