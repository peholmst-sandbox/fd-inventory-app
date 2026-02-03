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
 * An abandoned inventory check in a terminal state with resume capability.
 *
 * <p>When a check cannot be completed (e.g., apparatus dispatched, firefighter
 * reassigned, or other operational needs), it is abandoned. Partial progress
 * is preserved for reference.
 *
 * <p>Per BR-04, abandoned checks can be resumed within 30 minutes of abandonment.
 * After the resume window expires, the check cannot be resumed.
 *
 * @param id the unique identifier of this check
 * @param apparatusId the apparatus that was being checked
 * @param stationId the station where the apparatus is located
 * @param performedById the user who was performing the check
 * @param startedAt when the check was started
 * @param progress the partial check progress at time of abandonment
 * @param abandonedAt when the check was abandoned
 * @param reason the reason for abandoning the check (nullable)
 */
public record AbandonedCheck(
        InventoryCheckId id,
        ApparatusId apparatusId,
        StationId stationId,
        UserId performedById,
        Instant startedAt,
        CheckProgress progress,
        Instant abandonedAt,
        String reason
) implements InventoryCheck {

    public AbandonedCheck {
        Objects.requireNonNull(id, "Check ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
        Objects.requireNonNull(performedById, "Performer ID cannot be null");
        Objects.requireNonNull(startedAt, "Started at cannot be null");
        Objects.requireNonNull(progress, "Progress cannot be null");
        Objects.requireNonNull(abandonedAt, "Abandoned at cannot be null");

        if (abandonedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Abandoned at cannot be before started at");
        }
    }

    @Override
    public CheckStatus status() {
        return CheckStatus.ABANDONED;
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
     * Checks if this abandoned check can be resumed.
     *
     * <p>Per BR-04, abandoned checks can be resumed within 30 minutes.
     *
     * @param now the current timestamp to compare against
     * @return true if the check can be resumed
     */
    public boolean isResumable(Instant now) {
        Duration sinceAbandonment = Duration.between(abandonedAt, now);
        return sinceAbandonment.toMinutes() < InProgressCheck.RESUME_WINDOW_MINUTES;
    }

    /**
     * Resumes this abandoned check, transitioning back to in-progress.
     *
     * <p>Per BR-04, can only be resumed within 30 minutes of abandonment.
     *
     * @param resumedAt when the check was resumed
     * @return an in-progress check
     * @throws InventoryCheckException.ResumeWindowExpiredException if the resume window has expired
     */
    public InProgressCheck resume(Instant resumedAt) {
        if (!isResumable(resumedAt)) {
            throw new InventoryCheckException.ResumeWindowExpiredException(id);
        }
        return new InProgressCheck(
                id, apparatusId, stationId, performedById, startedAt,
                progress, resumedAt, null
        );
    }

    /**
     * Calculates the duration of the check from start to abandonment.
     *
     * @return the duration before abandonment
     */
    public Duration duration() {
        return Duration.between(startedAt, abandonedAt);
    }

    /**
     * Checks if any partial progress was made before abandonment.
     *
     * @return true if at least one item was verified
     */
    public boolean hasPartialProgress() {
        return progress.verifiedCount() > 0;
    }

    /**
     * Returns the number of items that were verified before abandonment.
     *
     * @return the count of verified items
     */
    public int itemsVerifiedBeforeAbandonment() {
        return progress.verifiedCount();
    }

    /**
     * Returns the number of items that were not verified.
     *
     * @return the count of unverified items
     */
    public int itemsNotVerified() {
        return progress.remainingItems();
    }

    /**
     * Calculates the percentage of completion before abandonment.
     *
     * @return the completion percentage (0-100)
     */
    public int completionPercentage() {
        return progress.progressPercentage();
    }

    /**
     * Checks if any issues were found in the partial progress.
     *
     * @return true if issues were found before abandonment
     */
    public boolean foundIssuesBeforeAbandonment() {
        return progress.hasIssues();
    }
}
