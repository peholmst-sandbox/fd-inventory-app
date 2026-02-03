package com.example.firestock.domain.audit;

/**
 * Status of a formal audit throughout its lifecycle.
 *
 * <p>A formal audit transitions through the following states:
 * <ul>
 *   <li>{@link #IN_PROGRESS} - Audit is active and items are being verified</li>
 *   <li>{@link #COMPLETED} - All items audited and audit is finalized (terminal state)</li>
 *   <li>{@link #ABANDONED} - Audit was cancelled before completion (terminal state)</li>
 * </ul>
 */
public enum AuditStatus {
    /**
     * Audit is currently in progress. Items can be added and verified.
     * The audit can be paused and resumed while in this state.
     */
    IN_PROGRESS,

    /**
     * Audit has been completed. All items have been audited and the audit
     * is now read-only. This is a terminal state.
     */
    COMPLETED,

    /**
     * Audit was abandoned before completion. Partial findings are preserved
     * but the audit cannot be resumed. This is a terminal state.
     */
    ABANDONED
}
