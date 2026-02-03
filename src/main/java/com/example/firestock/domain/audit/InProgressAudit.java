package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An in-progress formal audit that can be modified.
 *
 * <p>This is the active state of an audit where items can be verified, and the
 * audit can be paused, resumed, completed, or abandoned. State transitions
 * are modeled as methods that return new audit types:
 *
 * <ul>
 *   <li>{@link #complete(Instant)} - Transitions to {@link CompletedAudit}</li>
 *   <li>{@link #abandon(String, Instant)} - Transitions to {@link AbandonedAudit}</li>
 *   <li>{@link #pause(Instant)} - Returns new InProgressAudit with pausedAt set</li>
 *   <li>{@link #resume(Instant)} - Returns new InProgressAudit with pausedAt cleared</li>
 * </ul>
 *
 * @param id the unique identifier of this audit
 * @param apparatusId the apparatus being audited
 * @param auditorId the user performing the audit
 * @param startedAt when the audit was started
 * @param progress the current audit progress
 * @param pausedAt when the audit was paused (null if not paused)
 * @param lastActivityAt when the last audit activity occurred
 * @param notes free-form notes about the audit (nullable)
 */
public record InProgressAudit(
        FormalAuditId id,
        ApparatusId apparatusId,
        UserId auditorId,
        Instant startedAt,
        AuditProgress progress,
        Instant pausedAt,
        Instant lastActivityAt,
        String notes
) implements FormalAudit {

    /**
     * The number of days after which an audit is considered stale per BR-04.
     */
    public static final int STALENESS_DAYS = 7;

    public InProgressAudit {
        Objects.requireNonNull(id, "Audit ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(auditorId, "Auditor ID cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
        Objects.requireNonNull(progress, "Progress cannot be null");
        Objects.requireNonNull(lastActivityAt, "Last activity at cannot be null");
    }

    /**
     * Creates a new in-progress audit.
     *
     * @param id the audit ID
     * @param apparatusId the apparatus being audited
     * @param auditorId the user performing the audit
     * @param startedAt when the audit started
     * @param totalItems the total number of items to audit
     * @return a new in-progress audit
     */
    public static InProgressAudit start(
            FormalAuditId id,
            ApparatusId apparatusId,
            UserId auditorId,
            Instant startedAt,
            int totalItems
    ) {
        return new InProgressAudit(
                id,
                apparatusId,
                auditorId,
                startedAt,
                AuditProgress.initial(totalItems),
                null,
                startedAt,
                null
        );
    }

    @Override
    public AuditStatus status() {
        return AuditStatus.IN_PROGRESS;
    }

    /**
     * Checks if the audit is currently paused.
     *
     * @return true if the audit is paused
     */
    public boolean isPaused() {
        return pausedAt != null;
    }

    /**
     * Returns the paused timestamp as an Optional.
     *
     * @return the paused timestamp, or empty if not paused
     */
    public Optional<Instant> pausedAtOpt() {
        return Optional.ofNullable(pausedAt);
    }

    /**
     * Checks if this audit is stale per BR-04.
     *
     * <p>An audit is considered stale if no activity has occurred in the last 7 days.
     *
     * @param now the current timestamp to compare against
     * @return true if the audit is stale
     */
    public boolean isStale(Instant now) {
        Duration sinceLastActivity = Duration.between(lastActivityAt, now);
        return sinceLastActivity.toDays() >= STALENESS_DAYS;
    }

    /**
     * Pauses this audit.
     *
     * @param pausedAt when the audit was paused
     * @return a new in-progress audit with the paused timestamp set
     * @throws AuditException.AuditAlreadyPausedException if already paused
     */
    public InProgressAudit pause(Instant pausedAt) {
        if (isPaused()) {
            throw new AuditException.AuditAlreadyPausedException(id);
        }
        return new InProgressAudit(
                id, apparatusId, auditorId, startedAt,
                progress, pausedAt, pausedAt, notes
        );
    }

    /**
     * Resumes this paused audit.
     *
     * @param resumedAt when the audit was resumed
     * @return a new in-progress audit with the paused timestamp cleared
     * @throws AuditException.AuditNotPausedException if not paused
     */
    public InProgressAudit resume(Instant resumedAt) {
        if (!isPaused()) {
            throw new AuditException.AuditNotPausedException(id);
        }
        return new InProgressAudit(
                id, apparatusId, auditorId, startedAt,
                progress, null, resumedAt, notes
        );
    }

    /**
     * Completes this audit, transitioning to a completed state.
     *
     * <p>Per BR-03, all items must be audited before completion.
     *
     * @param completedAt when the audit was completed
     * @return a completed audit
     * @throws AuditException.IncompleteAuditException if not all items are audited
     */
    public CompletedAudit complete(Instant completedAt) {
        if (!progress.isAllAudited()) {
            throw new AuditException.IncompleteAuditException(id, progress.remainingItems());
        }
        return new CompletedAudit(
                id, apparatusId, auditorId, startedAt,
                progress, completedAt
        );
    }

    /**
     * Abandons this audit, transitioning to an abandoned state.
     *
     * <p>Partial findings are preserved in the abandoned audit.
     *
     * @param reason the reason for abandoning the audit
     * @param abandonedAt when the audit was abandoned
     * @return an abandoned audit
     */
    public AbandonedAudit abandon(String reason, Instant abandonedAt) {
        return new AbandonedAudit(
                id, apparatusId, auditorId, startedAt,
                progress, abandonedAt, reason
        );
    }

    /**
     * Creates a copy with updated progress.
     *
     * @param newProgress the new progress
     * @param activityAt when the activity occurred
     * @return a new in-progress audit with updated progress
     */
    public InProgressAudit withProgress(AuditProgress newProgress, Instant activityAt) {
        return new InProgressAudit(
                id, apparatusId, auditorId, startedAt,
                newProgress, pausedAt, activityAt, notes
        );
    }

    /**
     * Creates a copy with updated notes.
     *
     * @param newNotes the new notes
     * @return a new in-progress audit with updated notes
     */
    public InProgressAudit withNotes(String newNotes) {
        return new InProgressAudit(
                id, apparatusId, auditorId, startedAt,
                progress, pausedAt, lastActivityAt, newNotes
        );
    }

    /**
     * Returns the notes as an Optional.
     *
     * @return the notes, or empty if not set
     */
    public Optional<String> notesOpt() {
        return Optional.ofNullable(notes);
    }

    /**
     * Creates a copy with one more item audited.
     *
     * @param hasIssue whether the audited item has an issue
     * @param activityAt when the activity occurred
     * @return a new in-progress audit with updated progress
     */
    public InProgressAudit withItemAudited(boolean hasIssue, Instant activityAt) {
        return withProgress(progress.withItemAudited(hasIssue), activityAt);
    }

    /**
     * Creates a copy with one more unexpected item recorded.
     *
     * @param hasIssue whether the unexpected item has an issue
     * @param activityAt when the activity occurred
     * @return a new in-progress audit with updated progress
     */
    public InProgressAudit withUnexpectedItem(boolean hasIssue, Instant activityAt) {
        return withProgress(progress.withUnexpectedItem(hasIssue), activityAt);
    }
}
