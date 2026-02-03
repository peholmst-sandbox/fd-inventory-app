package com.example.firestock.domain.issue;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.jooq.enums.IssueStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * An acknowledged issue that has been seen by maintenance and is awaiting work.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@link #startWork(Instant)} - Transitions to {@link InProgressIssue}</li>
 *   <li>{@link #close(Instant)} - Transitions to {@link ClosedIssue}</li>
 * </ul>
 *
 * @param id the unique identifier of this issue
 * @param referenceNumber the human-readable reference number
 * @param target the target of the issue (equipment, consumable, or apparatus-level)
 * @param apparatusId the apparatus where the issue was found
 * @param stationId the station where the issue was found
 * @param title the issue title
 * @param description the detailed description
 * @param severity the severity level
 * @param category the issue category
 * @param reportedById the user who reported the issue
 * @param reportedAt when the issue was reported
 * @param isCrewResponsibility whether this issue is a crew responsibility
 * @param acknowledgedById the user who acknowledged the issue
 * @param acknowledgedAt when the issue was acknowledged
 */
public record AcknowledgedIssue(
        IssueId id,
        ReferenceNumber referenceNumber,
        IssueTarget target,
        ApparatusId apparatusId,
        StationId stationId,
        String title,
        String description,
        IssueSeverity severity,
        IssueCategory category,
        UserId reportedById,
        Instant reportedAt,
        boolean isCrewResponsibility,
        UserId acknowledgedById,
        Instant acknowledgedAt
) implements Issue {

    public AcknowledgedIssue {
        Objects.requireNonNull(id, "Issue ID cannot be null");
        Objects.requireNonNull(referenceNumber, "Reference number cannot be null");
        Objects.requireNonNull(target, "Issue target cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(stationId, "Station ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(reportedById, "Reported by ID cannot be null");
        Objects.requireNonNull(reportedAt, "Reported at cannot be null");
        Objects.requireNonNull(acknowledgedById, "Acknowledged by ID cannot be null");
        Objects.requireNonNull(acknowledgedAt, "Acknowledged at cannot be null");
    }

    @Override
    public IssueStatus status() {
        return IssueStatus.ACKNOWLEDGED;
    }

    /**
     * Starts work on this issue, transitioning to {@link InProgressIssue}.
     *
     * @param startedAt when work was started
     * @return an in-progress issue
     */
    public InProgressIssue startWork(Instant startedAt) {
        Objects.requireNonNull(startedAt, "Started at cannot be null");

        return new InProgressIssue(
                id,
                referenceNumber,
                target,
                apparatusId,
                stationId,
                title,
                description,
                severity,
                category,
                reportedById,
                reportedAt,
                isCrewResponsibility,
                acknowledgedById,
                acknowledgedAt,
                startedAt
        );
    }
}
