package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.CheckStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An in-progress inventory check that can be modified.
 *
 * <p>This is the active state of an inventory check where items can be verified,
 * and the check can be completed or abandoned. State transitions are modeled as
 * methods that return new check types:
 *
 * <ul>
 *   <li>{@link #complete(Instant)} - Transitions to {@link CompletedCheck}</li>
 *   <li>{@link #abandon(String, Instant)} - Transitions to {@link AbandonedCheck}</li>
 * </ul>
 *
 * @param id the unique identifier of this check
 * @param apparatusId the apparatus being checked
 * @param stationId the station where the apparatus is located
 * @param performedById the user performing the check
 * @param startedAt when the check was started
 * @param progress the current check progress
 * @param lastActivityAt when the last check activity occurred
 * @param notes free-form notes about the check (nullable)
 */
public record InProgressCheck(
        InventoryCheckId id,
        ApparatusId apparatusId,
        StationId stationId,
        UserId performedById,
        Instant startedAt,
        CheckProgress progress,
        Instant lastActivityAt,
        String notes
) implements InventoryCheck {

    /**
     * The number of hours after which an in-progress check is auto-abandoned per BR-03.
     */
    public static final int AUTO_ABANDON_HOURS = 4;

    /**
     * The number of minutes within which an abandoned check can be resumed per BR-04.
     */
    public static final int RESUME_WINDOW_MINUTES = 30;

    public InProgressCheck {
        Objects.requireNonNull(id, "Check ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
        Objects.requireNonNull(performedById, "Performer ID cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
        Objects.requireNonNull(progress, "Progress cannot be null");
        Objects.requireNonNull(lastActivityAt, "Last activity at cannot be null");
    }

    /**
     * Creates a new in-progress check.
     *
     * @param id the check ID
     * @param apparatusId the apparatus being checked
     * @param stationId the station where the apparatus is located
     * @param performedById the user performing the check
     * @param startedAt when the check started
     * @param totalItems the total number of items to verify
     * @return a new in-progress check
     */
    public static InProgressCheck start(
            InventoryCheckId id,
            ApparatusId apparatusId,
            StationId stationId,
            UserId performedById,
            Instant startedAt,
            int totalItems
    ) {
        return new InProgressCheck(
                id,
                apparatusId,
                stationId,
                performedById,
                startedAt,
                CheckProgress.initial(totalItems),
                startedAt,
                null
        );
    }

    @Override
    public CheckStatus status() {
        return CheckStatus.IN_PROGRESS;
    }

    /**
     * Checks if this check should be auto-abandoned per BR-03.
     *
     * <p>A check is considered stale if no activity has occurred in the last 4 hours.
     *
     * @param now the current timestamp to compare against
     * @return true if the check should be auto-abandoned
     */
    public boolean shouldAutoAbandon(Instant now) {
        Duration sinceLastActivity = Duration.between(lastActivityAt, now);
        return sinceLastActivity.toHours() >= AUTO_ABANDON_HOURS;
    }

    /**
     * Checks if this check is resumable within the resume window.
     *
     * <p>This is mainly for consistency - an in-progress check is always "resumable"
     * as it hasn't been abandoned yet.
     *
     * @param now the current timestamp
     * @return true (always, since check is in progress)
     */
    public boolean isResumable(Instant now) {
        return true;
    }

    /**
     * Completes this check, transitioning to a completed state.
     *
     * <p>Per BR-02, all items must be verified before completion.
     *
     * @param completedAt when the check was completed
     * @return a completed check
     * @throws InventoryCheckException.IncompleteCheckException if not all items are verified
     */
    public CompletedCheck complete(Instant completedAt) {
        if (!progress.isAllVerified()) {
            throw new InventoryCheckException.IncompleteCheckException(id, progress.remainingItems());
        }
        return new CompletedCheck(
                id, apparatusId, stationId, performedById, startedAt,
                progress, completedAt
        );
    }

    /**
     * Abandons this check, transitioning to an abandoned state.
     *
     * <p>Partial progress is preserved in the abandoned check. The check can be
     * resumed within 30 minutes per BR-04.
     *
     * @param reason the reason for abandoning the check (nullable)
     * @param abandonedAt when the check was abandoned
     * @return an abandoned check
     */
    public AbandonedCheck abandon(String reason, Instant abandonedAt) {
        return new AbandonedCheck(
                id, apparatusId, stationId, performedById, startedAt,
                progress, abandonedAt, reason
        );
    }

    /**
     * Creates a copy with updated progress.
     *
     * @param newProgress the new progress
     * @param activityAt when the activity occurred
     * @return a new in-progress check with updated progress
     */
    public InProgressCheck withProgress(CheckProgress newProgress, Instant activityAt) {
        return new InProgressCheck(
                id, apparatusId, stationId, performedById, startedAt,
                newProgress, activityAt, notes
        );
    }

    /**
     * Creates a copy with updated notes.
     *
     * @param newNotes the new notes
     * @return a new in-progress check with updated notes
     */
    public InProgressCheck withNotes(String newNotes) {
        return new InProgressCheck(
                id, apparatusId, stationId, performedById, startedAt,
                progress, lastActivityAt, newNotes
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
     * Creates a copy with one more item verified.
     *
     * @param hasIssue whether the verified item has an issue
     * @param activityAt when the activity occurred
     * @return a new in-progress check with updated progress
     */
    public InProgressCheck withItemVerified(boolean hasIssue, Instant activityAt) {
        return withProgress(progress.withItemVerified(hasIssue), activityAt);
    }
}
