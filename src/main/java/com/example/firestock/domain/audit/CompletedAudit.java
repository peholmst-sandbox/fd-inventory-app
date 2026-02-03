package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.AuditStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A completed formal audit in a read-only terminal state.
 *
 * <p>Per BR-07, completed audits cannot be modified. This record has no
 * mutation methods to enforce this constraint at compile time.
 *
 * <p>Completed audits preserve all findings and progress information for
 * historical reference and reporting.
 *
 * @param id the unique identifier of this audit
 * @param apparatusId the apparatus that was audited
 * @param auditorId the user who performed the audit
 * @param startedAt when the audit was started
 * @param progress the final audit progress (all items audited)
 * @param completedAt when the audit was completed
 */
public record CompletedAudit(
        FormalAuditId id,
        ApparatusId apparatusId,
        UserId auditorId,
        Instant startedAt,
        AuditProgress progress,
        Instant completedAt
) implements FormalAudit {

    public CompletedAudit {
        Objects.requireNonNull(id, "Audit ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(auditorId, "Auditor ID cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
        Objects.requireNonNull(progress, "Progress cannot be null");
        Objects.requireNonNull(completedAt, "Completed at cannot be null");

        if (!progress.isAllAudited()) {
            throw new IllegalArgumentException(
                    "Completed audit must have all items audited, but " +
                    progress.remainingItems() + " items remain");
        }

        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Completed at cannot be before started at");
        }
    }

    @Override
    public AuditStatus status() {
        return AuditStatus.COMPLETED;
    }

    /**
     * Calculates the duration of the audit from start to completion.
     *
     * @return the duration of the audit
     */
    public Duration duration() {
        return Duration.between(startedAt, completedAt);
    }

    /**
     * Checks if the audit found any issues.
     *
     * @return true if any items had issues
     */
    public boolean foundIssues() {
        return progress.hasIssues();
    }

    /**
     * Checks if any unexpected items were found during the audit.
     *
     * @return true if unexpected items were discovered
     */
    public boolean foundUnexpectedItems() {
        return progress.hasUnexpectedItems();
    }

    /**
     * Returns the total number of items audited.
     *
     * @return the count of audited items
     */
    public int totalItemsAudited() {
        return progress.totalItems();
    }

    /**
     * Returns the number of issues found during the audit.
     *
     * @return the count of items with issues
     */
    public int issueCount() {
        return progress.issuesFoundCount();
    }
}
