package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;

import java.time.Instant;

/**
 * Sealed interface representing a formal audit (aggregate root).
 *
 * <p>A formal audit is a comprehensive equipment inspection performed by maintenance
 * technicians on a specific apparatus. Unlike shift inventory checks, formal audits
 * include functional tests, condition assessments, and expiry tracking.
 *
 * <p>The sealed interface enforces state-based behavior through its permitted implementations:
 * <ul>
 *   <li>{@link InProgressAudit} - Active audit that can be modified, paused, resumed, completed, or abandoned</li>
 *   <li>{@link CompletedAudit} - Terminal read-only state (BR-07)</li>
 *   <li>{@link AbandonedAudit} - Terminal state preserving partial findings</li>
 * </ul>
 *
 * <p>State transitions:
 * <pre>
 * InProgressAudit --complete()--> CompletedAudit
 * InProgressAudit --abandon()---> AbandonedAudit
 * InProgressAudit --pause()-----> InProgressAudit (with pausedAt)
 * InProgressAudit --resume()----> InProgressAudit (pausedAt cleared)
 * </pre>
 */
public sealed interface FormalAudit permits InProgressAudit, CompletedAudit, AbandonedAudit {

    /**
     * Returns the unique identifier of this audit.
     *
     * @return the audit ID
     */
    FormalAuditId id();

    /**
     * Returns the apparatus being audited.
     *
     * @return the apparatus ID
     */
    ApparatusId apparatusId();

    /**
     * Returns the user who initiated the audit.
     *
     * @return the auditor's user ID
     */
    UserId auditorId();

    /**
     * Returns when the audit was started.
     *
     * @return the start timestamp
     */
    Instant startedAt();

    /**
     * Returns the current progress of the audit.
     *
     * @return the audit progress
     */
    AuditProgress progress();

    /**
     * Returns the current status of the audit.
     *
     * @return the audit status
     */
    AuditStatus status();

    /**
     * Checks if this audit is in a terminal state (completed or abandoned).
     *
     * @return true if the audit cannot be modified
     */
    default boolean isTerminal() {
        return status() == AuditStatus.COMPLETED || status() == AuditStatus.ABANDONED;
    }

    /**
     * Checks if this audit is active (in progress).
     *
     * @return true if the audit can be modified
     */
    default boolean isActive() {
        return status() == AuditStatus.IN_PROGRESS;
    }
}
