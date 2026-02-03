package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.AuditStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An abandoned formal audit in a terminal state.
 *
 * <p>When an audit cannot be completed (e.g., apparatus dispatched, technician
 * reassigned, or other operational needs), it is abandoned. Partial findings
 * are preserved for reference but the audit cannot be resumed.
 *
 * <p>This record has no mutation methods as it represents a terminal state.
 *
 * @param id the unique identifier of this audit
 * @param apparatusId the apparatus that was being audited
 * @param auditorId the user who was performing the audit
 * @param startedAt when the audit was started
 * @param progress the partial audit progress at time of abandonment
 * @param abandonedAt when the audit was abandoned
 * @param reason the reason for abandoning the audit (nullable)
 */
public record AbandonedAudit(
        FormalAuditId id,
        ApparatusId apparatusId,
        UserId auditorId,
        Instant startedAt,
        AuditProgress progress,
        Instant abandonedAt,
        String reason
) implements FormalAudit {

    public AbandonedAudit {
        Objects.requireNonNull(id, "Audit ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(auditorId, "Auditor ID cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
        Objects.requireNonNull(progress, "Progress cannot be null");
        Objects.requireNonNull(abandonedAt, "Abandoned at cannot be null");

        if (abandonedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Abandoned at cannot be before started at");
        }
    }

    @Override
    public AuditStatus status() {
        return AuditStatus.ABANDONED;
    }

    /**
     * Returns the reason for abandonment as an Optional.
     *
     * @return the reason, or empty if not provided
     */
    public Optional<String> reasonOpt() {
        return Optional.ofNullable(reason);
    }

    /**
     * Calculates the duration of the audit from start to abandonment.
     *
     * @return the duration before abandonment
     */
    public Duration duration() {
        return Duration.between(startedAt, abandonedAt);
    }

    /**
     * Checks if any partial findings were captured before abandonment.
     *
     * @return true if at least one item was audited
     */
    public boolean hasPartialFindings() {
        return progress.auditedCount() > 0;
    }

    /**
     * Returns the number of items that were audited before abandonment.
     *
     * @return the count of audited items
     */
    public int itemsAuditedBeforeAbandonment() {
        return progress.auditedCount();
    }

    /**
     * Returns the number of items that were not audited.
     *
     * @return the count of unaudited items
     */
    public int itemsNotAudited() {
        return progress.remainingItems();
    }

    /**
     * Calculates the percentage of completion before abandonment.
     *
     * @return the completion percentage (0-100)
     */
    public int completionPercentageAtAbandonment() {
        return progress.progressPercentage();
    }

    /**
     * Checks if any issues were found in the partial findings.
     *
     * @return true if issues were found before abandonment
     */
    public boolean foundIssuesBeforeAbandonment() {
        return progress.hasIssues();
    }
}
